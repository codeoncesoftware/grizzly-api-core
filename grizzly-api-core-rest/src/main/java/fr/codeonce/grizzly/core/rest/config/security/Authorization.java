/*
 * Copyright © 2020 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.codeonce.grizzly.core.rest.config.security;

import fr.codeonce.grizzly.core.domain.Organization.Member;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.user.AccountType;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authorization")
public class Authorization {
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    UserRepository userRepository;

    public boolean checkOrganisationAdmin(String id) {
        String email = SecurityContextUtil.getCurrentUserEmail();
        User user;
        if (!email.contains("@")) {
            user = userRepository
                    .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
            email = user.getEmail();
        } else {
            user = userRepository.findByEmail(email)
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
        }

        Member member = memberRepository.findByEmail(email);
        return member.getOrganisationId().equals(id) && member.getRole().equals("admin")
                && (user.getAccountType().equals(AccountType.LIMITLESS)
                || user.getAccountType().equals(AccountType.SCALABILITY) || user.getAccountType().equals(AccountType.FREE));
    }

    public boolean checkAddOrganisation() {
        String email = SecurityContextUtil.getCurrentUserEmail();
        User user;
        if (!email.contains("@")) {
            user = userRepository
                    .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
        } else {
            user = userRepository.findByEmail(email)
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
        }
        return user.getAccountType().equals(AccountType.LIMITLESS)
                || user.getAccountType().equals(AccountType.SCALABILITY) || user.getAccountType().equals(AccountType.FREE);
    }

    public boolean checkGetOrganisationRight() {
        String email = SecurityContextUtil.getCurrentUserEmail();
        User user;
        if (!email.contains("@")) {
            user = userRepository
                    .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
            email = user.getEmail();
        } else {
            user = userRepository.findByEmail(email)
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email));
        }
        Member member = memberRepository.findByEmail(email);

        return member.getRole().equals("admin") && (user.getAccountType().equals(AccountType.LIMITLESS)
                || user.getAccountType().equals(AccountType.SCALABILITY) || user.getAccountType().equals(AccountType.FREE));

    }

}

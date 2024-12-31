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
package fr.codeonce.grizzly.core.service.auth;

import fr.codeonce.grizzly.core.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

//	@Override
//	public UserDetails loadUserByUsername(String username) {
//		Optional<fr.codeonce.grizzly.core.domain.user.User> user = repository.findByEmail(username);
//		if (!user.isPresent()) {
//			throw new UsernameNotFoundException("User not found");
//		} else {
//			return new User(user.get().getEmail(), new BCryptPasswordEncoder().encode(user.get().getPassword()),
//					Arrays.asList(new SimpleGrantedAuthority("user")));
//		}
//
//	}

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        fr.codeonce.grizzly.core.domain.user.User user = repository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        return new User(user.getEmail(), new BCryptPasswordEncoder().encode(user.getPassword()), Collections.EMPTY_SET);
    }

}

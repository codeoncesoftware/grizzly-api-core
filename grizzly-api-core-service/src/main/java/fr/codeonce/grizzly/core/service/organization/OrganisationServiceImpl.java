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
package fr.codeonce.grizzly.core.service.organization;

import fr.codeonce.grizzly.core.domain.Organization.*;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class OrganisationServiceImpl implements IOrganizationService {


    @Autowired
    OrganisationRepository organisationRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    IMembreService memberService;

    @Override
    public Organization saveOrganisation(Organization organisation) {
        String currentEmail = SecurityContextUtil.getCurrentUserEmail();
        organisation.setTotalMembers(1L);
        organisation.setTotalTeams(0L);
        Organization savedOrganisation = organisationRepository.save(organisation);
        memberService.saveMembre(new Member(currentEmail, "admin", null, savedOrganisation.getId()));
        return savedOrganisation;
    }


    @Override
    public List<Organization> findAllOrganisations() {
        List<Organization> organisations = organisationRepository.findAll();
        organisations.forEach(organisation -> {
            organisation.setTotalMembers(memberRepository.countByOrganisationId(organisation.getId()));
            organisation.setTotalTeams(teamRepository.countByOrganisationId(organisation.getId()));
        });
        return organisations;
    }

    @Override
    public Organization findById(String id) {
        Organization organisation = organisationRepository.findById(id).get();
        organisation.setTotalMembers(memberRepository.countByOrganisationId(organisation.getId()));
        organisation.setTotalTeams(teamRepository.countByOrganisationId(organisation.getId()));
        return organisation;
    }

    @Override
    public Organization updateOrganisation(Organization organisation, String id) {
        organisation.setId(id);
        return organisationRepository.save(organisation);
    }

    @Override
    public void deleteOrganisation(String id) {
        memberService.deletAllMemebersOfOrganisation(id);
        organisationRepository.deleteById(id);
    }

}

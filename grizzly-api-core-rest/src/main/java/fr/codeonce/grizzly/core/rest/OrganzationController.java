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
package fr.codeonce.grizzly.core.rest;

import fr.codeonce.grizzly.core.domain.Organization.*;
import fr.codeonce.grizzly.core.service.organization.IMembreService;
import fr.codeonce.grizzly.core.service.organization.IOrganizationService;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organisation")
public class OrganzationController {

    private static final Logger log = LoggerFactory.getLogger(OrganzationController.class);
    @Autowired
    IOrganizationService organisationService;
    @Autowired
    OrganisationRepository organisationRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    IMembreService memberService;

    @GetMapping("/all")
    public List<Organization> getAllOrganisations() {
        return organisationService.findAllOrganisations();
    }

    @PreAuthorize("@authorization.checkOrganisationAdmin(#id)")
    @GetMapping("/find/{id}")
    public Organization getOrganisation(@PathVariable String id) {
        return organisationService.findById(id);
    }

    @PreAuthorize("@authorization.checkAddOrganisation()")
    @PostMapping("/add")
    public Organization addOrganisation(@RequestBody Organization organisation) {
        return organisationService.saveOrganisation(organisation);
    }

    @PreAuthorize("@authorization.checkOrganisationAdmin(#id)")
    @PutMapping("/update/{id}")
    public Organization updateOrganisation(@RequestBody Organization organisation, @PathVariable String id) {
        return organisationService.updateOrganisation(organisation, id);
    }

    @PreAuthorize("@authorization.checkOrganisationAdmin(#id)")
    @DeleteMapping("/delete/{id}")
    public void deleteOrganisation(@PathVariable String id) {
        organisationService.deleteOrganisation(id);
    }

    @GetMapping("/only/{name}")
    public Boolean onlyOrganisation(@PathVariable String name) {
        return organisationRepository.existsByNameIgnoreCase(name);
    }

    @GetMapping("onlyOrganisation/{email}")
    public Boolean onlyOrganisationByEmail(@PathVariable String email) {
        return memberService.checkMembreInOrganisation(email);
    }

    @GetMapping("onlyOrganisationAndEmail/{organisationId}/{email}")
    public Boolean onlyOrganisationByEmailAndOrganisationID(@PathVariable String organisationId, @PathVariable String email) {
        return memberRepository.existsByOrganisationIdAndEmail(organisationId, email);
    }

    @GetMapping("/myOrganisationTeams")
    public List<Team> teamsOfMyOrganisation() {
        return teamRepository.findByOrganisationId(memberRepository.findByEmail(SecurityContextUtil.getCurrentUserEmail()).getOrganisationId());
    }

}

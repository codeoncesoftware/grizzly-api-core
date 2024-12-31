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

import fr.codeonce.grizzly.core.domain.Organization.Member;
import fr.codeonce.grizzly.core.domain.Organization.Organization;
import fr.codeonce.grizzly.core.domain.Organization.Team;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.service.organization.IMembreService;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    @Autowired
    IMembreService memberService;

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    @GetMapping("/team")
    public Set<Project> getAllMicroservicesInTeamHub() {
        String currentEmail = SecurityContextUtil.getCurrentUserEmail();
        Member membre = memberService.findMemberByEmail(currentEmail);
        List<Member> members = memberService.findTeamsMembers(membre.getTeamIds());
        return memberService.findAllMembersSubscriptions(members);
    }

    @GetMapping("/all")
    public List<Member> getAllMemebers() {
        return memberService.findAllMembres();
    }

    @GetMapping("/find/{id}")
    public Member getMemeber(@PathVariable String id) {
        return memberService.findById(id);
    }

    @GetMapping("/findByOrganisation/{id}")
    public List<Member> getMemeberByOrganisation(@PathVariable String id) {
        return memberService.findByOrganisationId(id);
    }

    @GetMapping("/findByTeam/{id}")
    public List<Member> getMemeberByTeam(@PathVariable String id) {
        return memberService.findByTeamId(id);
    }

    @PostMapping("/add")
    public Member addMember(@RequestBody Member membre) {
        return memberService.saveMembre(membre);
    }

    @PutMapping("/update/{id}")
    public Member updateMember(@RequestBody Member membre, @PathVariable String id) {
        return memberService.updateMembre(membre, id);
    }

    @DeleteMapping("delete/{id}")
    public void deleteMemeber(@PathVariable String id) {
        memberService.deleteMembre(id);
    }

    @PreAuthorize("@authorization.checkGetOrganisationRight()")
    @GetMapping("/currentUserOrganisation")
    public List<Organization> getCurrentUserOrganisation() throws Exception {
        String currentEmail = SecurityContextUtil.getCurrentUserEmail();

        try {
            return memberService.findOrganisationByEmail(currentEmail);
        } catch (Exception e) {
            return new ArrayList<Organization>();
        }
    }

    @GetMapping("/currentUserTeam")
    public List<Team> getCurrentUserTeam() throws Exception {
        String currentEmail = SecurityContextUtil.getCurrentUserEmail();
        try {
            return memberService.findTeamByEmail(currentEmail);
        } catch (Exception e) {
            return new ArrayList<Team>();
        }
    }

    @GetMapping("/currentUserIsAdmin")
    public boolean checkUserIsAdmin() throws Exception {
        String currentEmail = SecurityContextUtil.getCurrentUserEmail();
        try {
            return memberService.checkUserIsAdmin(currentEmail);
        } catch (Exception e) {
            return false;
        }
    }

    @DeleteMapping("/deleteAllMemebers/organisationId")
    public void deleteAllMEmebers(String organisationId) {
        memberService.deletAllMemebersOfOrganisation(organisationId);
    }

    @GetMapping("/membreExistsInMembers/{email}")
    public Boolean membreExistsInMembers(@PathVariable String email) {
        return memberService.membreExistsInMembers(email);
    }

    @GetMapping("/membreExistsInUsers/{email}")
    public Boolean membreExistsInUsers(@PathVariable String email) {
        return memberService.membreExistsInUsers(email);
    }

    @GetMapping("/addMemberToTeam/{teamId}/{email}")
    public Member addMemberToTeam(@PathVariable String teamId, @PathVariable String email) {
        return memberService.addMemberToTeam(teamId, email);
    }

}

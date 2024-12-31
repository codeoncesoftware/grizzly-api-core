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
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class MemberServiceImpl implements IMembreService {

    @Autowired
    MemberRepository membreRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrganisationRepository organisationRepository;
    @Autowired
    TeamRepository teamRepository;

    @Override
    public Member saveMembre(Member membre) {
        membre.setTeamIds(new ArrayList<String>());
        return membreRepository.save(membre);
    }

    @Override
    public Boolean membreExistsInUsers(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public Boolean membreExistsInMembers(String email) {
        return membreRepository.existsByEmail(email);
    }

    @Override
    public List<Member> findAllMembres() {
        return membreRepository.findAll();
    }

    @Override
    public Member findById(String id) {
        return membreRepository.findById(id).get();
    }

    @Override
    public Member updateMembre(Member membre, String id) {
        membre.setId(id);
        return membreRepository.save(membre);
    }

    @Override
    public void deleteMembre(String id) {
        membreRepository.deleteById(id);
    }

    @Override
    public List<Member> findByOrganisationId(String organisationId) {
        return membreRepository.findAllByOrganisationId(organisationId);

    }

    @Override
    public List<Member> findByTeamId(String teamIds) {
        return membreRepository.findAllByTeamIds(teamIds);

    }

    @Override
    public List<Organization> findOrganisationByEmail(String email) throws Exception {
        List<Organization> organisations = new ArrayList<>();
        Member member = membreRepository.findByEmail(email);
        try {
            organisations.add(organisationRepository.findById(member.getOrganisationId()).orElseThrow(
                    GlobalExceptionUtil.notFoundException(Organization.class, member.getOrganisationId())));
        } catch (NullPointerException e) {
            throw new Exception("cet utilisateur n'est inscrit à aucune organisation");
        }
        return organisations;
    }

    @Override
    public List<Team> findTeamByEmail(String email) throws Exception {
        List<Team> teams = new ArrayList<Team>();
        Member member = membreRepository.findByEmail(email);
        try {
            member.getTeamIds().forEach(teamId -> teams.add(teamRepository.findById(teamId).get()));
        } catch (NullPointerException e) {
            throw new Exception("cet utilisateur n'est inscrit à aucune équipe");
        }
        return teams;

    }

    @Override
    public void deletAllMemebersOfOrganisation(String organisationId) {

        membreRepository.findAllByOrganisationId(organisationId)
                .forEach(member -> membreRepository.deleteById(member.getId()));
    }

    @Override
    public Boolean checkMembreInOrganisation(String email) {
        if (membreRepository.existsByEmail(email)) {
            Member member = membreRepository.findByEmail(email);
            return organisationRepository.existsById(member.getOrganisationId());
        }
        return false;
    }

    @Override
    public Member addMemberToTeam(String teamId, String email) {
        Member member = membreRepository.findByEmail(email);
        List<String> teams = member.getTeamIds();
        teams.add(teamId);
        return membreRepository.save(member);

    }

    @Override
    public Member findMemberByEmail(String email) {
        return membreRepository.findByEmail(email);
    }

    @Override
    public List<Member> findTeamsMembers(List<String> teamsIds) {
        return membreRepository.findAllByTeamIdsIn(teamsIds);
    }

    @Override
    public boolean checkUserIsAdmin(String currentEmail) {
        Member member = membreRepository.findByEmail(currentEmail);
        return member.getRole().equals("admin");
    }

    @Override
    public Set<Project> findAllMembersSubscriptions(List<Member> members) {
        return Collections.emptySet();
    }
}

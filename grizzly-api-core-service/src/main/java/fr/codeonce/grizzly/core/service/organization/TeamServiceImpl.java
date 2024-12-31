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

import fr.codeonce.grizzly.core.domain.Organization.Member;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.Organization.Team;
import fr.codeonce.grizzly.core.domain.Organization.TeamRepository;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class TeamServiceImpl implements ITeamService {
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    IMembreService membreService;

    @Override
    public List<Team> findAllTeams() {
        List<Team> teams = teamRepository.findAll();
        teams.forEach(team -> team.setTotalMembers(memberRepository.countByTeamIds(team.getId())));
        return teams;
    }

    @Override
    public Team addTeam(Team team, String adminEmail) {
        team.setTotalMembers(team.getTotalMembers() + 1);
        team.setOwner(adminEmail);
        Team teamCreated = teamRepository.save(team);

        Member member = memberRepository.findByEmail(adminEmail);
        member.getTeamIds().add(teamCreated.getId());
        memberRepository.save(member);
        return teamCreated;
    }

    @Override
    public Team updateTeam(Team team, String id) {
        team.setId(id);
        return teamRepository.save(team);

    }

    @Override
    public List<Team> findByOrganisationId(String organisationId) {
        List<Team> teams = teamRepository.findByOrganisationId(organisationId);
        teams.forEach(team -> team.setTotalMembers(memberRepository.countByTeamIds(team.getId())));
        return teams;
    }

    @Override
    public void deleteMemberFromTeam(String idTeam, String email) {
        Member member = memberRepository.findByEmail(email);
        List<String> teams = member.getTeamIds();
        teams.remove(idTeam);
        member.setTeamIds(teams);
        memberRepository.save(member);
    }

    @Override
    public Team findById(String id) {
        Team team = teamRepository.findById(id).orElseThrow(GlobalExceptionUtil.notFoundException(Team.class, id));
        team.setTotalMembers(memberRepository.countByTeamIds(team.getId()));
        return team;
    }

    @Override
    public void deleteTeam(String id) {
        membreService.findByTeamId(id).forEach(member -> deleteMemberFromTeam(id, member.getEmail()));
        teamRepository.deleteById(id);
    }
}

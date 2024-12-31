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
import fr.codeonce.grizzly.core.domain.Organization.Organization;
import fr.codeonce.grizzly.core.domain.Organization.Team;
import fr.codeonce.grizzly.core.domain.project.Project;

import java.util.List;
import java.util.Set;

public interface IMembreService {
    public Member saveMembre(Member membre);

    public List<Member> findAllMembres();

    public List<Member> findByOrganisationId(String organisationId);

    public Member findById(String id);

    public Member updateMembre(Member membre, String id);

    public void deleteMembre(String id);

    public List<Member> findByTeamId(String teamId);

    public List<Organization> findOrganisationByEmail(String email) throws Exception;

    public List<Team> findTeamByEmail(String email) throws Exception;

    public void deletAllMemebersOfOrganisation(String organisationId);

    public Boolean checkMembreInOrganisation(String email);

    public Boolean membreExistsInUsers(String email);

    public Boolean membreExistsInMembers(String email);

    public Member addMemberToTeam(String teamId, String email);

    public Member findMemberByEmail(String email);

    public List<Member> findTeamsMembers(List<String> teamsIds);

    public boolean checkUserIsAdmin(String currentEmail);

    public Set<Project> findAllMembersSubscriptions(List<Member> members);

}

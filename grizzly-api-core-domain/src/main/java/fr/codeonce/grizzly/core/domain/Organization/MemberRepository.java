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
package fr.codeonce.grizzly.core.domain.Organization;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {
    public List<Member> findAllByOrganisationId(String organisationId);

    public List<Member> findAllByTeamIds(String teamIds);

    public Member findByEmail(String Email);

    public Boolean existsByEmail(String email);

    public List<Member> findAllByTeamIdsIn(List<String> teamsIds);

    public long countByTeamIds(String id);

    public Object countByOrganisationId(String id);

    public Boolean existsByOrganisationIdAndEmail(String organisationId, String email);

}
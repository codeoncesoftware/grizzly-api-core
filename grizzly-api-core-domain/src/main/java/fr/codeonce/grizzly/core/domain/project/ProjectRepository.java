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
package fr.codeonce.grizzly.core.domain.project;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    Optional<Project> findByNameIgnoreCaseAndUserEmail(String projectName, String email);

    boolean existsByNameIgnoreCaseAndUserEmail(String projectName, String email);

    boolean existsByDatabaseName(String databaseName);

    void deleteByName(String projectName);

    List<Project> findAllByUserEmail(String userEmail);

    List<Project> findByDbsourceId(String dbSourceId);

    Long countByUserEmail(String email);

    void deleteByNameAndUserEmail(String name, String userEmail);

    List<Project> findAll();

    List<Project> findByUserEmailAndUsed(String userEmail, boolean used);

    List<Project> findByTypeIgnoreCaseAndUserEmail(String type, String userEmail);

    List<Project> findAllByAuthMSRuntimeURL(String authMSRuntimeURL);

}

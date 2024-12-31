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

import fr.codeonce.grizzly.core.domain.Organization.Organization;

import java.util.List;

public interface IOrganizationService {
    public Organization saveOrganisation(Organization organisation);

    public List<Organization> findAllOrganisations();

    public Organization findById(String id);

    public Organization updateOrganisation(Organization organisation, String id);

    public void deleteOrganisation(String id);

}

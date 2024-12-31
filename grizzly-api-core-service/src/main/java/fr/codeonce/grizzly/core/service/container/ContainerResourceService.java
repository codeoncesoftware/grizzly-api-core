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
package fr.codeonce.grizzly.core.service.container;

import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.resource.CustomQuery;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContainerResourceService {


    @Autowired
    private ContainerRepository containerRepository;

    /**
     * Fetch a Resource From DB based on the Container ID and The Resource Path
     *
     * @param containerId
     * @param resourcePath
     * @return Resource
     */
    public Resource getResource(String containerId, String resourcePath) {
        Optional<Container> opContainer = containerRepository.findById(containerId);
        if (opContainer.isPresent()) {
            Container container = opContainer.get();
            return container.getResources().stream().filter(res -> res.getPath().equalsIgnoreCase(resourcePath))
                    .findFirst().orElseThrow(GlobalExceptionUtil.notFoundException(Resource.class, resourcePath));
        }
        throw GlobalExceptionUtil.notFoundException(Resource.class, resourcePath).get();
    }

    public Boolean getUniqueResource(String containerId, String path, String method) {
        Boolean unique = true;
        return unique;
    }


}

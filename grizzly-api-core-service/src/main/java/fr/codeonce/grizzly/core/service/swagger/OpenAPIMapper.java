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
package fr.codeonce.grizzly.core.service.swagger;

import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.domain.resource.ResourceGroup;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OpenAPIMapper {

    @Mapping(ignore = true, target = "components")
    @Mapping(source = "container.name", target = "info.version")
    OpenAPI mapToOpenAPI(Container container);

    @Mapping(source = "resourceGroup.name", target = "name")
    @Mapping(source = "resourceGroup.description", target = "description")
    Tag mapToTag(ResourceGroup resourceGroup);

    @Mapping(ignore = true, target = "parameters")
    @Mapping(ignore = true, target = "responses")
    Container mapToContainer(OpenAPI openapi, String content) throws Exception;

    @Mapping(ignore = true, target = "parameters")
    @Mapping(ignore = true, target = "responses")
    @Mapping(ignore = true, target = "requestBody")
    Operation mapToOperation(Resource resource);

    @Mapping(ignore = true, target = "responses")
    @Mapping(ignore = true, target = "requestBody")
    @Mapping(ignore = true, target = "parameters")
    Resource mapToResource(Operation operation, OpenAPI swagger);

}

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
package fr.codeonce.grizzly.core.service.resource.utils;

import fr.codeonce.grizzly.common.runtime.resource.*;
import fr.codeonce.grizzly.core.domain.resource.APIResponse;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.domain.resource.ResourceGroup;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceRuntimeMapper {

    RuntimeResource mapToRuntime(Resource resource);

    default List<RuntimeResourceFunction> mapToFunction(List<String> ids) {
        return null;

    }

    ;

    default List<String> mapToString(List<RuntimeResourceFunction> functions) {
        return null;

    }

    ;

    RuntimeResourceParameter mapToRuntime(ResourceParameter resource);

    Resource mapToResource(CreateResourceRequest resource);

    ResourceGroup mapToResourceGroup(ResourceGroupDto resource);

    RuntimeAPIResponse aPIResponseToRuntimeAPIResponse(APIResponse aPIResponse);
}

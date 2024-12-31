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
package fr.codeonce.grizzly.core.service.swagger.utils;

import fr.codeonce.grizzly.core.domain.container.Container;
import io.swagger.v3.oas.models.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComponentGenerator {
    public Map<String, Schema> generateSchemaDefiniton(Container container) {
        Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();
        container.getEndpointModels().forEach(model -> {
            Schema schema = new Schema();
            schema.setRequired(model.getRequired());
            schema.setEnum(model.getEnums());
            schema.setDescription(model.getDescription());
            schema.setType(model.getType());
            schema.setTitle(model.getTitle());
            schemas.put(model.getTitle(), schema);
        });

        return schemas;
    }

}

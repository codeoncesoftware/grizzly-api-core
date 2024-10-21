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

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.codeonce.grizzly.core.domain.container.Container;
import io.swagger.models.Swagger;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface ISwaggerGenerator {

    public String generate(Container container, String type) throws JsonProcessingException;

    public String swaggerToJson(Swagger swagger) throws JsonProcessingException;

    public Container mapOpenAPIToContainer(OpenAPI openapi, String projectId, String content, String editor, boolean docker) throws Exception;

    public Container mapSwaggerToContainer(Swagger swagger, String projectId, String content, String editor, boolean docker) throws Exception;

    /**
     * Parse a MultipartFile and construct a Swagger Object
     *
     * @param file
     * @return a Swagger Object
     */
    public Swagger getSwagger(MultipartFile file);

    public Swagger getSwagger(InputStream inputStream) throws IOException;


}

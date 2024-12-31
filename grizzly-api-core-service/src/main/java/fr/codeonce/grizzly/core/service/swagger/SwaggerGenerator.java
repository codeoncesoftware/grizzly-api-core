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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.InvalidSwagger;
import fr.codeonce.grizzly.core.domain.container.InvalidSwaggerRepository;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;


@Service
public class SwaggerGenerator implements ISwaggerGenerator {


    @Autowired
    private InvalidSwaggerRepository invalidSwaggerRepository;
    @Autowired
    private SwaggerMapperService swaggerMapperService;

    @Autowired
    private OpenAPIMapperService openapiMapperService;

    @Autowired
    private MappingJackson2HttpMessageConverter springMvcJacksonConverter;

    @Transactional
    public String generate(Container container, String type) throws JsonProcessingException {
        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return jsonWriter.writeValueAsString(swaggerMapperService.mapToSwagger(container, type));
    }

    @Transactional
    public String generateOpenAPI(Container container, String type) {
        return Yaml.pretty(openapiMapperService.mapToOpenAPI(container, type));
    }

    @Transactional
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String generateSwaggerOrOpenapi(Container container, String type, String version) throws JsonProcessingException {
        if (version.equalsIgnoreCase("V2")) {
            return generate(container, type);
        } else {
            return Yaml.pretty(openapiMapperService.mapToOpenAPI(container, type));
        }
    }

    public String swaggerToJson(Swagger swagger) throws JsonProcessingException {
        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return jsonWriter.writeValueAsString(swagger);
    }

    @Transactional
    public Container mapSwaggerToContainer(Swagger swagger, String projectId, String content, String editor, boolean docker) throws Exception {
        return swaggerMapperService.mapToContainer(swagger, projectId, content, editor, docker);

    }

    @Transactional
    public Container mapOpenAPIToContainer(OpenAPI openapi, String projectId, String content, String editor, boolean docker) throws Exception {
        return openapiMapperService.mapToContainer(openapi, projectId, content, editor, docker);
    }

    /**
     * Parse a MultipartFile and construct a Swagger Object
     *
     * @param file
     * @return a Swagger Object
     */
    public Swagger getSwagger(MultipartFile file) {
        try {
            return getSwagger(file.getInputStream());
        } catch (IOException e) {
            GlobalExceptionUtil.fileNotFoundException(file.getOriginalFilename());
            return null;
        }
    }

    public String getType(String cont) throws JsonProcessingException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(cont, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        String content = jsonWriter.writeValueAsString(obj);
        JsonNode jsonBody = jsonWriter.readTree(content);
        if (jsonBody.has("openapi")) {
            return "openapi";
        } else if (jsonBody.has("swagger")) {
            return "swagger";
        } else {
            InvalidSwagger invalidSwagger = new InvalidSwagger();
            invalidSwagger.setContent(content);
            invalidSwaggerRepository.save(invalidSwagger);
            throw new NoSuchElementException("Invalid Swagger for more details contact us on support@codeonce.fr");
        }
    }

    public Swagger getSwagger(InputStream inputStream) throws IOException {
        // Get File Content
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        String filecontent = writer.toString();
        // Prepare the Swagger Object
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(filecontent, Object.class);
        ObjectMapper jsonWriter = new ObjectMapper();
        return new SwaggerParser()
                .read(springMvcJacksonConverter.getObjectMapper().readTree(jsonWriter.writeValueAsString(obj)));
    }

    public OpenAPI getOpenAPI(String filecontent) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(filecontent, Object.class);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(obj);
        return new OpenAPIV3Parser().readContents(jsonAsYaml).getOpenAPI();

    }

}

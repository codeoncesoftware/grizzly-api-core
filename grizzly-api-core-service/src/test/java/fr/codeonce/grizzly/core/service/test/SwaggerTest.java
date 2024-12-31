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
package fr.codeonce.grizzly.core.service.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;
import fr.codeonce.grizzly.core.service.swagger.SwaggerGenerator;
import fr.codeonce.grizzly.core.service.swagger.utils.ParameterFactory;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SwaggerTest {

    @Autowired
    private SwaggerGenerator swaggerGenerator;

    @Test
    public void testParameterFactory() {
        ResourceParameter resourceParameter = new ResourceParameter();
        resourceParameter.setName("testParameter");
        resourceParameter.setType("testType");
        resourceParameter.setValue("0");
        resourceParameter.setIn("Body");
        ParameterFactory factory = ParameterFactory.getInstance();
        assertTrue(factory.makeParameter(resourceParameter) instanceof BodyParameter);
        resourceParameter.setIn("Header");
        assertTrue(factory.makeParameter(resourceParameter) instanceof HeaderParameter);
        resourceParameter.setIn("formData");
        assertTrue(factory.makeParameter(resourceParameter) instanceof FormParameter);
        resourceParameter.setIn("Query");
        assertTrue(factory.makeParameter(resourceParameter) instanceof QueryParameter);
        resourceParameter.setIn("should return path Parameter");
        assertTrue(factory.makeParameter(resourceParameter) instanceof PathParameter);
    }

//    @Test
//    public void testGetSwaggerFromFile() throws IOException, JsonProcessingException {
//
//        File swaggerFile = ResourceUtils.getFile("classpath:swagger" + File.separator + "swagger.json");
//
//        InputStream is = FileUtils.openInputStream(swaggerFile);
//        // Verify Swagger Object
//        Swagger swagger = swaggerGenerator.getSwagger(is);
//        Assert.assertNotNull("swagger object should not be null", swagger);
//
//        // Verify Swagger Content
//        String filecontent = new String(Files.readAllBytes(Paths.get(swaggerFile.getPath()))).replaceAll("\\s+", "");
//        String swaggerJson = swaggerGenerator.swaggerToJson(swagger);
//        Assert.assertTrue("method should return same file content",
//                filecontent.equalsIgnoreCase(swaggerJson.replaceAll("\\s+", "")));
//    }

    // @Test
    public void testGetSwaggerWithSchema() throws IOException, JsonProcessingException {

        File swaggerFile = ResourceUtils.getFile("classpath:swagger" + File.separator + "swaggerWithSchema.json");

        InputStream is = FileUtils.openInputStream(swaggerFile);
        // Verify Swagger Object
        Swagger swagger = swaggerGenerator.getSwagger(is);
        Assertions.assertNotNull(swagger, "swagger object should not be null");

        String swaggerJson = swaggerGenerator.swaggerToJson(swagger);
        System.out.println(swaggerJson);

        // Verify Swagger Content
        String filecontent = new String(Files.readAllBytes(Path.of(swaggerFile.getPath())));
        Assertions.assertTrue(filecontent.equals(swaggerJson), "method should return same file content");
    }

}

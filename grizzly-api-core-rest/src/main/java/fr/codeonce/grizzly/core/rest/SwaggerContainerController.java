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
package fr.codeonce.grizzly.core.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.codeonce.grizzly.core.domain.container.InvalidSwaggerRepository;
import fr.codeonce.grizzly.core.service.container.ContainerSwaggerService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/swagger")
public class SwaggerContainerController {

    @Autowired
    private ContainerSwaggerService containerSwaggerService;

    private static final Logger log = LoggerFactory.getLogger(SwaggerContainerController.class);

    @Autowired
    private InvalidSwaggerRepository invalidSwaggerRepository;

    @GetMapping(value = "/{version}/{containerId}/{swaggerUuid}", produces = {"application/json"})
    public String getSwaggerOrOpenApi(@PathVariable String version, @PathVariable String containerId,
                                      @PathVariable String swaggerUuid) throws JsonProcessingException {
        log.info("request to download swagger for container with ID : {}", containerId);
        return containerSwaggerService.getSwaggerOrOpenApiJson("prod", version, containerId, swaggerUuid);
    }

    /**
     * Generate Swagger for a Given Container Id return generated File for DOWNLOAD
     *
     * @param containerId
     * @param response
     * @throws IOException
     */
    @GetMapping("/generateswagger/{type}/{containerId}")
    public void downloadGeneratedSwagger(HttpServletResponse response, @PathVariable String type,
                                         @PathVariable String containerId) throws IOException {
        log.info("request to download a {} swagger for container with ID : {}", type, containerId);
        containerSwaggerService.downloadSwaggerJsonFile(response, type, containerId, "swagger");
    }

    @GetMapping("/generateopenapi/{type}/{containerId}")
    public void downloadGeneratedOpenApi(HttpServletResponse response, @PathVariable String type,
                                         @PathVariable String containerId) throws IOException {
        log.info("request to download a {} swagger for container with ID : {}", type, containerId);
        containerSwaggerService.downloadSwaggerJsonFile(response, type, containerId, "openapi");
    }

    @GetMapping("/exportInvalidSwagger/{swaggerId}")
    public String exportInvalidSwagger(@PathVariable String swaggerId) throws IOException {
        return invalidSwaggerRepository.findById(swaggerId).get().getContent();
    }

}

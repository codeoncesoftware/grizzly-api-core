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

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.ResourceGroup;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.DBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.project.ProjectExample;
import fr.codeonce.grizzly.core.service.swagger.SwaggerGenerator;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import io.swagger.models.Swagger;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContainerSwaggerService {

    private static final String API_URL = "https://converter.swagger.io/api/convert";

    private static final String JSON = ".json";

    private static final String APP_JSON = "application/json";

    private static final String URL_NOT_VALID = "URL is not valid";

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private SwaggerGenerator swaggerGenerator;

    @Autowired
    private ProjectExample projectExample;

    @Autowired
    private ContainerMapperService containerMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private MongoDBSourceMapperService dbSourceMapperService;

    @Autowired
    private DBSourceService dbsourceService;

    private static final Logger log = LoggerFactory.getLogger(ContainerSwaggerService.class);

    /**
     * Generate Swagger From a Container from it's given ID
     *
     * @param containerId
     * @return Swagger File
     * @throws IOException
     */

    public void generateSwaggerForDocker(String containerId, String path) throws Exception {
        Optional<Container> container = containerRepository.findById(containerId);
        if (container.isPresent()) {
            String json = swaggerGenerator.generate(container.get(), "dev");
            String fileName = buildFileName(container.get());
            File f = new File(path + "/" + fileName + JSON);
            f.getParentFile().mkdirs();
            f.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
                writer.write(json);
            }
        }
    }

    public String getOpenApi(String content) {
        RestTemplate restTamplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", APP_JSON);
        HttpEntity<String> request = new HttpEntity<>(content, headers);
        return restTamplate.postForObject(API_URL, request, String.class);
    }

    public File generateSwaggerFile(Container container, String type, String json) {
        String fileName = buildFileName(container);
        if (type.equalsIgnoreCase("dev")) {
            fileName = fileName + "-dev";
        }
        fileName = fileName.concat(JSON);
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
            fw.flush();
            return new File(fileName);
        } catch (Exception e) {
            log.debug("error in json file {}:", e);
            return null;
        }
    }

    public File generateOpenApi(String type, String containerId) throws IOException {
        Container container = containerRepository.findById(containerId)//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        String json = swaggerGenerator.generateOpenAPI(container, type);
        return generateSwaggerFile(container, type, json);
    }

    public File generateSwagger(String type, String containerId) throws IOException {
        Container container = containerRepository.findById(containerId)//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        String json = swaggerGenerator.generate(container, type);
        return generateSwaggerFile(container, type, json);
    }

    private String buildFileName(Container container) {
        return projectRepository.findById(container.getProjectId())//
                .map(p -> StringUtils.joinWith("_", p.getName(), container.getName()))
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, container.getProjectId()));
    }

    /**
     * Set Swagger JSON file to the Response for download
     *
     * @throws IOException
     */
    public void downloadSwaggerJsonFile(HttpServletResponse response, String type, String containerId,
                                        String swaggerVersion) throws IOException {
        // Generate a Swagger.json File
        File file = null;
        if (swaggerVersion.equals("swagger")) {
            file = generateSwagger(type, containerId);
        } else {
            file = generateOpenApi(type, containerId);
        }
        response.setContentType(APP_JSON);
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment; filename=%s".formatted(file.getName()));
        response.setHeader("fileName", file.getName());
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
            // Delete the Generated File after Download
            FileUtils.deleteQuietly(file);
        } catch (Exception e) {
            throw GlobalExceptionUtil.fileNotFoundException(containerId).get();
        }
    }

    /**
     * Create a Container from a JSON Swagger File, save it in the DB and map it to
     * a ContainerDto
     *
     * @param file
     * @param projectId
     * @return ContainerDto to be previewed in the front-end
     * @throws Exception
     * @throws IOException
     */
    public ContainerDto importSwagger(MultipartFile file, String projectId, String containerName) throws Exception {
        Container container = saveSwaggerContainer(file, projectId, containerName);
        this.projectRepository.findById(projectId).ifPresent(proj -> {
            container.setDatabaseName(proj.getDatabaseName());
            container.setDbsourceId(proj.getDbsourceId());
        });
        if (container.getDbsourceId() != null) {
            this.dbSourceRepository.findById(container.getDbsourceId()).ifPresent(db -> {
                if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                    container.setDatabaseName(db.getPhysicalDatabase());
                }
            });
        }
        Container containerToSave = addDefaultAuthGoup(container);

        return containerMapper.mapToDto(containerRepository.save(containerToSave));
    }

    public ContainerDto importSwaggerOnExistingContainer(MultipartFile file, String containerId, String editor,
                                                         boolean docker) throws Exception {
        Container currentContainer = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));

        Project currentProject = this.projectRepository.findById(currentContainer.getProjectId())
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));

        InputStream inputStream = new BufferedInputStream(file.getInputStream());
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        String filecontent = writer.toString();
        String type = swaggerGenerator.getType(filecontent);
        Container container;
        if (type.equals("openapi")) {
            OpenAPI openapi = swaggerGenerator.getOpenAPI(filecontent);
            container = swaggerGenerator.mapOpenAPIToContainer(openapi, currentProject.getId(), filecontent, editor,
                    docker);
            container.setExportPreference("V3");
        } else {
            Swagger swagger = swaggerGenerator.getSwagger(file);
            container = swaggerGenerator.mapSwaggerToContainer(swagger, currentProject.getId(), filecontent, editor,
                    docker);

        }

        container.setId(containerId);
        container.setProjectId(currentContainer.getProjectId());
        container.setName(currentContainer.getName());
        container.setHierarchyId(currentContainer.getHierarchyId());
        container.setUserEmail(currentContainer.getUserEmail());
        container.setSwaggerUuid(currentContainer.getSwaggerUuid());
        container.setDbsourceId(currentContainer.getDbsourceId());
        container.setDatabaseName(currentContainer.getDatabaseName());

        if (docker) {
            container.setHost("localhost:8050/");
            container.setBasePath("runtime/" + containerId);
        }

        if (!editor.equals("true")) {
            addDefaultAuthGoup(container);
        }

        container = containerRepository.save(container);
        List<String> collectionNames = new ArrayList<>();
        container.getResources().forEach(resource -> {
            if (!collectionNames.contains(resource.getCustomQuery().getCollectionName())) {
                collectionNames.add(resource.getCustomQuery().getCollectionName());
            }
        });

        final String containerID = container.getId();
        collectionNames.forEach(collectionName -> dbsourceService.addNewCollection(containerID, collectionName));
        return containerMapper.mapToDto(container);
    }

    public Container saveSwaggerContainer(MultipartFile file, String projectId, String containerName) throws Exception {
        InputStream inputStream = new BufferedInputStream(file.getInputStream());
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        String filecontent = writer.toString();
        Container container = new Container();
        container.setProjectId(projectId);
        String type = swaggerGenerator.getType(filecontent);
        if (type.equals("openapi")) {
            OpenAPI openapi = swaggerGenerator.getOpenAPI(filecontent);
            // Custom Mapping
            container = swaggerGenerator.mapOpenAPIToContainer(openapi, projectId, filecontent, "false", false);
            container.setExportPreference("V3");
            if (containerName != null) {
                container.setName(containerName);
            } else if (openapi.getInfo() != null) {
                // Set the Version as Name
                container.setName(openapi.getInfo().getVersion());
            }
        } else {
            Swagger swagger = swaggerGenerator.getSwagger(file);
            String editorMode = "false";
            container = swaggerGenerator.mapSwaggerToContainer(swagger, projectId, filecontent, editorMode, false);
            if (containerName != null) {
                container.setName(containerName);
            } else if (swagger.getInfo() != null) {
                // Set the Version as Name
                container.setName(swagger.getInfo().getVersion());
            }
        }
        return container;
    }

    public String getSwaggerJson(String type, String containerId, String swaggerUuid) throws JsonProcessingException {
        Optional<Container> container = containerRepository.findById(containerId);
        if (container.isPresent() && container.get().getSwaggerUuid().equals(swaggerUuid)) {
            return this.swaggerGenerator.generate(container.get(), type);
        } else {
            throw new IllegalArgumentException(URL_NOT_VALID);
        }
    }

    public String getSwaggerOrOpenApiJson(String type, String version, String containerId, String swaggerUuid) throws JsonProcessingException {
        Optional<Container> container = containerRepository.findById(containerId);
        if (container.isPresent() && container.get().getSwaggerUuid().equals(swaggerUuid)) {
            return this.swaggerGenerator.generateSwaggerOrOpenapi(container.get(), type, version);
        } else {
            throw new IllegalArgumentException(URL_NOT_VALID);
        }
    }

    public String getSwagger(String type, String containerId) throws JsonProcessingException {
        Optional<Container> container = containerRepository.findById(containerId);
        if (container.isPresent()) {
            return this.swaggerGenerator.generate(container.get(), type);
        } else {
            throw new IllegalArgumentException(URL_NOT_VALID);
        }
    }

    private Container addDefaultAuthGoup(Container container) {
        Project project = projectRepository.findById(container.getProjectId())
                .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, container.getProjectId()));
        if (project.getAuthMSRuntimeURL() == null && !project.getType().equals("authentication microservice") &&
                project.isSecurityEnabled()) {
            ResourceGroup security = new ResourceGroup();
            security.setName("Authentication Grizzly");
            security.setDescription("JWT token");
            container.getResourceGroups().add(0, security);
            DBSource db = dbSourceRepository.findById(project.getDbsourceId())
                    .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, project.getDbsourceId()));
            DBSourceDto savedDataSource = dbSourceMapperService.mapToDto(db);
            container.getResources().addAll(0, projectExample.createAuthGroup(false, savedDataSource, security.getName()));
        }
        return container;
    }

}

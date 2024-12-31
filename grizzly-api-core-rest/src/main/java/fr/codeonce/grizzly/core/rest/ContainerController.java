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

import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.container.*;
import fr.codeonce.grizzly.core.service.oauth2identityprovider.KeycloakOauthService;
import fr.codeonce.grizzly.core.service.project.ProjectService;
import fr.codeonce.grizzly.core.service.project.SecurityApiConfigDto;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/container")
public class ContainerController {

    private static final Logger log = LoggerFactory.getLogger(ContainerController.class);

    @Autowired
    private ContainerService containerService;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ContainerSwaggerService containerSwaggerService;

    @Autowired
    private ContainerImportService containerImportService;

    @Autowired
    private ContainerExportService containerExportService;

    @Autowired
    private ContainerCloneService containerCloneService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ContainerResourceService containerResourceService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private KeycloakOauthService keycloakOauthService;

    /**
     * Creates a new container given a container DTO
     *
     * @param containerDto
     * @return ContainerDto
     * @throws CustomGitAPIException
     */
    @PostMapping("/create")
    public ContainerDto saveContainer(@RequestBody ContainerDto containerDto) throws CustomGitAPIException {
        return containerService.saveContainer(containerDto, false);
    }

    /**
     * Check if the new container's name is unique in a project
     *
     * @param containerDto
     * @return ContainerDto
     */
    @PostMapping("/check")
    public boolean existsContainerName(@RequestBody ContainerDto containerDto) {
        log.info("request to check container name unicity for {} ", containerDto.getName());
        return containerService.existsContainerName(containerDto);
    }

    /**
     * Returns a Container given its containerId
     *
     * @param containerId
     * @return ContainerDto
     */
    @GetMapping("/{containerId}")
    public ContainerDto get(@PathVariable String containerId) {
        log.info("request to get container with ID {} ", containerId);
        return containerService.get(containerId);
    }

    /**
     * Returns a list of all the containers in the database
     *
     * @return List<ContainerDto>
     */
    @GetMapping("/all")
    public List<ContainerDto> getAll() {
        log.info("request to get all containers");
        return containerService.getAll();
    }

    /**
     * Returns a list of all the containers that belong to a specified project given
     * its projectId
     *
     * @param projectId
     * @return
     */
    @GetMapping("/project/{projectId}")
    public List<ContainerDto> containersByProject(@PathVariable String projectId) {
        log.info("request to get containers for project with ID : {} ", projectId);
        return containerService.containersByProject(projectId);
    }

    /**
     * Deletes a container given its containerId
     *
     * @param containerId
     */
    @DeleteMapping("/delete/{containerId}")
    public void delete(@PathVariable String containerId) {
        log.info("request to delete container with ID : {} ", containerId);
        containerService.delete(containerId);
    }

    /**
     * Deletes all the Containers in the DB
     */
    @DeleteMapping("/deleteAll")
    public void deleteAll() {
        log.info("request to delete all wontainers");
        containerService.deleteAll();
    }

    /**
     * Deletes all the containers under a project in the database for a Given
     * Project ID
     */
    @DeleteMapping("/deleteAll/project/{projectId}")
    public void deleteAllByProject(@PathVariable String projectId) {
        log.info("request to delete all containers for project with ID : {} ", projectId);
        containerService.deleteAllByProject(projectId);
    }

    /**
     * Generate a new Container From an uploaded Swagger JSON File
     *
     * @param file
     * @param projectId
     * @param containerName
     * @return a new ContainerDto for the fresh Container
     * @throws Exception
     * @throws IOException
     */
    @PostMapping("/importSwagger")
    public ContainerDto importSwagger(@RequestParam MultipartFile file, @RequestParam String projectId,
                                      @RequestParam(required = false) String containerName) throws Exception {
        log.info("request to import a swagger file for container with name : {} of project with ID : {}", containerName,
                projectId);
        return containerSwaggerService.importSwagger(file, projectId, containerName);
    }

    /**
     * Update the container to match the uploaded Swagger definition
     *
     * @param file
     * @param containerId
     * @return
     * @throws Exception
     */
    @PostMapping("/importSwaggerOnExistingContainer")
    public ContainerDto importSwaggerOnExistingContainer(@RequestParam MultipartFile file,
                                                         @RequestParam String containerId, @RequestParam String editor) throws Exception {
        log.info("request to import a swagger file for container with ID : {} ", containerId);
        return containerSwaggerService.importSwaggerOnExistingContainer(file, containerId, editor, false);
    }

    /**
     * Toggle container status, if active it will be used by kubernates (This
     * feature will be used in version 2)
     *
     * @param containerId
     */
    @GetMapping("/enableDisable/{containerId}")
    public void enableDisableContainer(@PathVariable String containerId) {
        log.info("request to enableDisable container with ID : {} ", containerId);
        containerService.enableDisableContainer(containerId);
    }

    /**
     * Clone an existing container to create next version of the micro-service
     *
     * @param containerId
     * @param newContainerName
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @PostMapping("/clone/{containerId}")
    public Object cloneContainer(@PathVariable String containerId, @RequestBody String newContainerName) throws IOException {
        log.info("request to clone container with ID : {} and new name as : {}", containerId, newContainerName);
        return containerCloneService.cloneContainer(containerId, newContainerName);
    }

    /**
     * Fetch a Resource based on the Container ID and the Resource Path to be
     * returned to ZUUL in order to Execute a Query API
     *
     * @param containerId
     * @param resourcePath
     * @return
     */
    @GetMapping("/getResource")
    public Resource getResource(@RequestParam String containerId,
                                @RequestParam String resourcePath) {
        log.info("request to fetch resource from container with ID : {} and resource path as : {}", containerId,
                resourcePath);
        return containerResourceService.getResource(containerId, resourcePath);
    }

    /**
     * Generate a ZIP file containing the Swagger file and the related uploaded
     * resources
     *
     * @param containerId
     * @param response
     * @throws JSONException
     * @throws IOException
     */
    @GetMapping(value = "/export/{containerId}", produces = "application/zip")
    public void export(@PathVariable String containerId, HttpServletResponse response) throws IOException {
        log.info("request to export container with ID : {}", containerId);

        File directory = containerExportService.export(containerId);
        List<String> fileList = containerExportService.getFileList(directory);
        containerExportService.zipContainer(fileList, containerId, directory, response);
    }

    @PostMapping("/import")
    public Object importContainer(@RequestParam MultipartFile file, @RequestParam String projectId,
                                  @RequestParam String containerName, @RequestParam String dbsourceId, @RequestParam String databaseName)
            throws Exception {
        // CONTROL HERE FILE SIZE
        long fileSize = file.getSize() / 1000; // in KiloBytes
        if (!analyticsService.storageLimitsOnUpload(fileSize)) {
            return new Document("code", "-1"); // limit reached!
        }
        log.info("request to import container for Project with ID : {}", projectId);
        return containerImportService.importContainer(file, containerName, projectId, dbsourceId, databaseName);
    }

    @GetMapping("/public/get")
    public Map<String, String> getContainerInfos(@RequestParam String containerId) {
        Container c = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        Map<String, String> map = new HashMap<>();
        map.put("dbsourceId", c.getDbsourceId());
        map.put("dbname", c.getDatabaseName());
        return map;
    }

    @GetMapping("/public/getroles")
    public List<String> getRoles(@RequestParam String containerId) {
        return projectService.get(containerService.get(containerId).getProjectId()).getRoles();
    }

    @GetMapping("/public/project")
    public String getProject(@RequestParam String containerId) {
        String projectId = containerService.get(containerId).getProjectId();
        return projectService.get(projectId).getDatabaseName();
    }

    @GetMapping("/public/security")
    public SecurityApiConfigDto getSecurityConfig(@RequestParam String containerId) {
        return projectService.get(containerService.get(containerId).getProjectId()).getSecurityConfig();
    }

    @GetMapping("/clear/{containerId}")
    public ContainerDto clearContainer(@PathVariable String containerId) {
        return containerService.clearContainer(containerId);
    }

    @PostMapping("/public/share/{containerId}")
    public void confirmEmail(@PathVariable String containerId, @RequestBody ShareContainerDto shareContainerDto,
                             HttpServletRequest req) throws IOException {
        log.info("request to confirm email token received");
        containerService.shareContainer(shareContainerDto, containerId, req.getHeader("Accept-Language"));
    }

    // generate SignIn and SignUp models for existing containers in prodDB for upgrade purposes
    @GetMapping("/generate/authModels")
    public void generateAuthModels() {
        containerService.generateAuthModels();
    }

    @GetMapping("/generate/exportPreference")
    public void generateExportPreference() {
        containerService.generateExportPreference();
    }

    @PostMapping("/generate-new-container")
    public ContainerDto generateNewContainer() {
        return containerService.generateNewContainer();
    }

    @PostMapping("/public/endpoint/authorization")
    public Map<String, Object> authorizationEndpoint(@RequestParam String username, @RequestParam String password, @RequestParam String containerId) throws ParseException {
        return keycloakOauthService.authorizationEndpoint(username, password, containerId);
    }

    @PostMapping("/createContainer/{dbSourceId}/{physicalDatabase}")
    public ContainerDto saveContainerWithAbsentProject(@RequestBody ContainerDto container, @PathVariable String dbSourceId,
                                                       @PathVariable String physicalDatabase) {
        return containerService.saveContainerWithAbsentProject(container, dbSourceId, physicalDatabase);
    }

    @GetMapping("/dbsource/{dbsourceId}")
    public List<ContainerDto> containersByDbsourceId(@PathVariable String dbsourceId) {
        log.info("request to get containers for dbsource with ID : {} ", dbsourceId);
        return containerService.containersByDbsource(dbsourceId);
    }

    @PutMapping("/updateCollectionNameCustomQuery/{currentCollectionName}/{newCollectionName}")
    public boolean updateCollectionNameCustomQuery(@PathVariable String currentCollectionName,
                                     @PathVariable String newCollectionName) {
        System.out.println("request to update collection name from " + currentCollectionName + " to " + newCollectionName  );
        containerService.updateCollectionName(currentCollectionName, newCollectionName);
        return true;
    }

    @GetMapping("/statCollection/{collectionName}")
    public boolean containersByCollection(@PathVariable String collectionName) {
        log.info("request to get if exist containers  with collectionName : {} ", collectionName);
        return containerService.containersByCollection(collectionName);
    }
}

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
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.project.EstimationDto;
import fr.codeonce.grizzly.core.service.project.ProjectDto;
import fr.codeonce.grizzly.core.service.project.ProjectService;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AnalyticsService analyticsService;

    @Value("${frontUrl}")
    private String url;

    /**
     * Check if the new project name is not taken
     *
     * @param name (the new project's name)
     * @return true if already exists, false if not
     */
    @GetMapping("/check/{name}")
    public boolean existsProjectName(@PathVariable String name) {
        log.info("Request for Checking project name unicity : {}", name);
        return projectService.existsProjectName(name, null);
    }

    @GetMapping("/check/{name}/{projectId}")
    public boolean existsProjectName(@PathVariable String name, @PathVariable String projectId) {
        log.info("Request for Checking project name unicity : {}", name);
        return projectService.existsProjectName(name, projectId);
    }


    @PostMapping("/git/check")
    public boolean checkGitcredentials(@RequestBody ProjectDto projectDto) {
        log.info("Request for Checking git credentials");
        return projectService.checkGitcredentials(projectDto);
    }

    @PostMapping("/git/sync")
    public ProjectDto syncGit(@RequestBody ProjectDto projectDto) {
        log.info("Request for Synchronizing git");
        projectService.gitSyncProject(projectDto);
        return projectDto;
    }

    /**
     * Creates a new project in the database with the given project DTO
     *
     * @param projectDto
     * @return ProjectDto
     * @throws CustomGitAPIException
     */
    @PostMapping("/create")
    public Object createProject(@RequestBody ProjectDto projectDto) throws CustomGitAPIException, Exception {
        log.info("Request for Creating new project with name :{}", projectDto.getName());

        Document metrics = analyticsService.checkUserLimits();
        if (!metrics.getBoolean("ms")) {
            return new Document().append("code", "-1").append("message", "Microservice limit reached!");
        }
        return projectService.createProject(projectDto);
    }

    /**
     * Returns a single project with its given id
     *
     * @param id
     * @return ProjectDto
     */
    @GetMapping("/{id}")
    public ProjectDto getProject(@PathVariable String id) {
        log.info("Request for Getting project with ID : {}", id);
        return projectService.get(id);
    }

    /**
     * Updates the old project with its given id and the new project DTO
     *
     * @param newProjectDto
     * @param projectId
     * @return ProjectDto
     */
    @PutMapping("/update/{projectId}")
    public ProjectDto update(@RequestBody ProjectDto newProjectDto, @PathVariable String projectId) {
        log.info("Request for Updating a project with ID : {}", projectId);
        return projectService.update(newProjectDto, projectId);
    }

    /**
     * Deletes a single project with its given id
     *
     * @param projectId
     * @return
     */
    @DeleteMapping("/delete/{projectId}")
    public void delete(@PathVariable String projectId) {
        log.info("Request for Deleting a project with ID : {}", projectId);
        this.projectService.delete(projectId);
    }

    /**
     * Deletes all the projects in the database
     *
     * @return
     */
    @DeleteMapping("/deleteAll")
    public void deleteAll() {
        log.info("Delete all projects Request");
        projectService.deleteAll();
    }

    @GetMapping("/export/{projectId}")
    public String exportProject(@PathVariable String projectId) throws IOException {
        log.info("Request to export a project with ID : {}", projectId);
        return projectService.exportProject(projectId);
    }

    /**
     * Returns a list of all the projects in the database
     *
     * @return List<ProjectDto>
     */
    @GetMapping("/all")
    public List<ProjectDto> getAll() {
        log.info("Request for Deleting all projects for the signed-in user");
        return projectService.getAllByUser();
    }

    @GetMapping("/existsByDatabaseName/{dbName}")
    public Boolean existsByDatabaseName(@PathVariable String dbName) {
        log.info("Request for Deleting all projects for the signed-in user");
        return projectService.existsProjectByDbSourceName(dbName);
    }

    @DeleteMapping("/deleteByNameAndEmail/{name}/{userEmail}")
    public String deleteByName(@PathVariable String name, @PathVariable String userEmail) {
        return projectService.deleteByNameAndUserEmail(name, userEmail);
    }

    @PostMapping("/public/clone/{projectId}/{containerId}")
    public Container cloneProjectAndContainer(@PathVariable String projectId, @PathVariable String containerId) {
        return projectService.cloneProjectAndContainer(projectId, containerId);
    }

    @GetMapping("/frontUrl")
    public String getFrontUrl() {
        return url;
    }

    // generate project type for existing project in prodDB for upgrade purposes
    @GetMapping("/generate/type")
    public void addProjectType() {
        projectService.addProjectType();
    }

    @GetMapping("/countUsedByUser/{userEmail}")
    public int countUsedPojectByUser(String userEmail) {
        return projectService.countUsedPojectByUserEmail(userEmail);
    }

    @PostMapping("/used/project/estimation")
    public EstimationDto getUsedProjectsInCurrentMonth(@RequestBody User user) {
        return projectService.getUsedProjectsInCurrentMonth(user);
    }

    @GetMapping("/getByType/{type}")
    public List<ProjectDto> getProjectByType(@PathVariable String type) {
        log.info("Request for getting all project with a given type ");
        return projectService.getByType(type);
    }

    @PostMapping("/addApp/{projectId}")
    public ProjectDto addApp(@PathVariable String projectId, @RequestBody String appName) {
        return projectService.addApp(projectId, appName);
    }

    @GetMapping("/deleteApp/{projectId}/{clientId}")
    public ProjectDto deleteApp(@PathVariable String projectId, @PathVariable String clientId) {
        return projectService.deleteApp(projectId, clientId);
    }

}

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

/**
 * Controller to Handle file's requests
 * Using {@link ResourceService}
 *
 * @author rayen
 */

import fr.codeonce.grizzly.common.runtime.resource.CreateResourceRequest;
import fr.codeonce.grizzly.common.runtime.resource.RuntimeResource;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.container.ContainerResourceService;
import fr.codeonce.grizzly.core.service.resource.ResourceService;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = {"*"}, allowedHeaders = {"*"})
@RequestMapping("/api/resource")

public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ContainerResourceService containerResourceService;

    @Autowired
    private AnalyticsService analyticsService;

    @PostMapping(value = "/getbranchslist")
    public List<String> getRepoBranchsList(@RequestBody String body) {

        log.debug("Request to get git repository branchs list");

        return this.resourceService.getRepoBranchsList(body);
    }

//	@PostMapping(value = "/importGitProject")
//	public String cloneGitRepo(@RequestBody ProjectDto projectDto) throws CustomGitAPIException {
//		// CONTROL HERE FILE SIZE
//
//		log.info("Request to clone GIT repository");
//		return this.resourceService.cloneGitRepository(projectDto);
//	}

    @PostMapping(value = "/sync")
    public String gitSync(@RequestParam List<MultipartFile> files, @RequestParam String gitRepoUrl,
                          @RequestParam String branch, @RequestParam String projectId, @RequestParam String containerId,
                          @RequestParam String swaggerUuid, @RequestParam String dbsourceId, @RequestParam String databaseName,
                          @RequestParam String gitUsername, @RequestParam String gitPassword)
            throws CustomGitAPIException, IOException {
        log.info("Request to clone GIT repository");
        return this.resourceService.gitSync(files, gitRepoUrl, branch, projectId, containerId, swaggerUuid, dbsourceId,
                databaseName, gitUsername, gitPassword);
    }

    @PostMapping(value = "/importZipProject")
    public String importZipFile(@RequestParam MultipartFile zipFile, @RequestParam String idContainer,
                                @RequestParam String dbsourceId, @RequestParam String databaseName) throws IOException {
        // CONTROL HERE FILE SIZE
        long fileSize = zipFile.getSize() / 1000; // in KiloBytes
        if (!analyticsService.storageLimitsOnUpload(fileSize)) {
            return "-1"; // limit reached!
        }
        log.info("Request to import ZIP File for container with ID : {}", idContainer);
        return this.resourceService.importZipFile(zipFile, idContainer, dbsourceId, databaseName);
    }

    @PostMapping(value = "/uploadFile")
    public String uploadFile(@RequestParam MultipartFile file, @RequestParam String containerId) throws IOException {
        return this.resourceService.uploadFile(file, containerId);
    }

    @DeleteMapping(value = "/delete/{containerId}")
    public void deleteFiles(@PathVariable String containerId) {
        this.resourceService.deleteFiles(containerId);
    }

    @GetMapping("/public")
    public RuntimeResource getResource(@RequestParam String containerId,
                                       @RequestParam String resourcePath, @RequestParam String method,
                                       @RequestParam String returnType) {
        log.info("Request to fetch resource from container with ID : {} and resource path is : {}", containerId,
                resourcePath);

        // DEBUG PERFORMANCE
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("request to fetch resource");

        RuntimeResource runtimeResource = this.resourceService.getRuntimeResource(containerId, resourcePath, method, returnType);

        // SHOW DEBUG
        stopWatch.stop();

        return runtimeResource;
    }

    @GetMapping("/public/getUniqueResource")
    public Boolean getUniqueResource(@RequestParam String containerId,
                                     @RequestParam String resourcePath, HttpServletRequest req) {
        log.info("request to fetch resource from container with ID : {} and resource path as : {}", containerId,
                resourcePath);
        return containerResourceService.getUniqueResource(containerId, resourcePath, req.getMethod());
    }

    @GetMapping("/public/getResource")
    public GridFsResource getResourceForTuntime(@RequestParam String containerId,
                                                @RequestParam String resourcePath) {
        return this.resourceService.getGridFsResource(this.resourceService.getResource(containerId, resourcePath),
                containerId);
    }

    @GetMapping("/public/getResourceWithId")
    public GridFsResource getResourceWithId(@RequestParam String containerId,
                                            @RequestParam String fileId) {
        return this.resourceService.getGridFsResource(this.resourceService.getResourceFileWithId(containerId, fileId),
                containerId);
    }

    @PostMapping("/public/resourceRequest")
    public RuntimeResource createResource(@RequestParam String containerId,
                                          @RequestBody CreateResourceRequest createResourceRequest) throws Exception {

        return resourceService.createResource(containerId, createResourceRequest);
        // return resourceService.createResource(containerId,createResourceRequest);

    }

}

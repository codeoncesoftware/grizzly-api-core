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

import fr.codeonce.grizzly.core.domain.docker.DockerExport;
import fr.codeonce.grizzly.core.service.docker.DockerExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerExportController {

    @Autowired
    DockerExportService dockerExportService;

    @PostMapping("/export/{containerId}")
    public DockerExport exportDocker(@RequestBody DockerExport dockerExport, @PathVariable String containerId) throws Exception {
        return dockerExportService.generateSwaggerForDocker(containerId, dockerExport);
    }

    @GetMapping("/findAll/{projectId}")
    public List<DockerExport> exportDocker(@PathVariable String projectId) throws Exception {
        return dockerExportService.findAllExportsByEmailAndProjectId(projectId);
    }

    @GetMapping("/find/{id}")
    public DockerExport findDockerExportById(@PathVariable String id) throws Exception {
        return dockerExportService.findExportsById(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDockerExportById(@PathVariable String id) throws Exception {
        dockerExportService.deleteDockerExportById(id);
    }

    @PostMapping("/update")
    public DockerExport updateDockerExportState(@RequestBody DockerExport dockerExport) throws Exception, IOException {
        return dockerExportService.saveDockerExportInfo(dockerExport);
    }
}

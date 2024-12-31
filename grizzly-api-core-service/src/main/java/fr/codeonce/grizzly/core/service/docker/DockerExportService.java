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
package fr.codeonce.grizzly.core.service.docker;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.docker.DockerExport;
import fr.codeonce.grizzly.core.domain.docker.DockerExportRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.service.container.ContainerSwaggerService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.DockerExportCustomException;
import fr.codeonce.grizzly.core.service.util.EmailService;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@ConditionalOnProperty(prefix = "aws", name = {"accessKey", "secretKey"})
@Service
public class DockerExportService {

    @Autowired
    private ContainerRepository containerRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private DockerExportRepository dockerExportRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    @Value("${frontUrl}")
    private String url;

    @Value("${environment}")
    private String environment;

    @Value("${aws.accessKey}")
    private String awsAccessKey;

    @Value("${aws.secretKey}")
    private String awsSecretKey;

    private static final Logger log = LoggerFactory.getLogger(ContainerSwaggerService.class);

    @Autowired
    ContainerSwaggerService containerSwaggerService;

    private String buildFileNameForDocker(Container container) {
        log.info(container.getName());
        return projectRepository.findById(container.getProjectId())//
                .map(p -> StringUtils.joinWith("=", p.getName().replaceAll(" ", ""), container.getName()))
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, container.getProjectId()));
    }

    public DockerExport generateSwaggerForDocker(String containerId, DockerExport dockerExport) throws Exception {
        Container container = containerRepository.findById(containerId)//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        String fileName = "";
        if (url.equals("http://localhost:4200")) {
            fileName = url.substring(7, url.length()) + "=";
        } else {
            fileName = url.substring(8, url.length()) + "=";
        }
        fileName += buildFileNameForDocker(container);
        dockerExport = dockerExportRepository.save(dockerExport);
        fileName += "=" + userService.getConnectedUserByApiKey().getId().substring(2, 8) + "="
                + userService.getConnectedUserByApiKey().getApiKey() + "=" + dockerExport.getId() + "="
                + dockerExport.getTag() + "=" + dockerExport.getDatabaseType();
        if (fileName.charAt(fileName.length() - 1) == '=') {
            fileName = fileName.substring(0, fileName.length() - 2);
        }
        File file = containerSwaggerService.generateOpenApi("dev", containerId);
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .withRegion(Regions.EU_CENTRAL_1).build();
        s3client.putObject("codeonce-swaggers-repo", "IN/" + environment + "/" + fileName, file);
        return dockerExport;
    }

    public DockerExport saveDockerExportInfo(DockerExport dockerExport) throws IOException {
        dockerExport = dockerExportRepository.save(dockerExport);
        sendDockerExportInfo(dockerExport.getUserEmail(), dockerExport);
        return dockerExport;
    }

    public List<DockerExport> findAllExportsByEmailAndProjectId(String projectId) {
        return dockerExportRepository.findAllByUserEmailAndProjectId(userService.getConnectedUserEmail(), projectId);
    }

    public DockerExport findExportsById(String id) {
        return dockerExportRepository.findById(id)//
                .orElseThrow(GlobalExceptionUtil.notFoundException(DockerExport.class, id));

    }

    public void deleteDockerExportById(String id) throws DockerExportCustomException {
        try {
            dockerExportRepository.deleteById(id);
        } catch (Exception e) {
            throw new DockerExportCustomException("Docker delete error ", e);
        }
    }

    public void sendDockerExportInfo(String userEmail, DockerExport dockerExport) throws IOException {
        String subject = "Docker exported successfully";
        String content = emailService.getOutputDockerMailContent("docker-export.html", "templates/docker-export", "en",
                dockerExport);
        emailService.send(content, subject, userEmail, "api");

    }
}

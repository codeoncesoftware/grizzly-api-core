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
package fr.codeonce.grizzly.core.service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.core.domain.docker.DockerExport;
import fr.codeonce.grizzly.core.service.container.ShareContainerDto;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String userFrom;
    @Value("${spring.mail.personal}")
    private String personal;
    @Value("${frontUrl}")
    private String frontUrl;
    @Value("${functionImageUrl : functionImageUrlTest}")
    private String functionImageUrl;

    @Autowired
    private ResourceLoader resourceloader;

    @Autowired
    private SpringTemplateEngine templateEngine;


    public boolean send(String content, String subject, String email, String type) {
        log.info("send email with subject: {} to email: {}", subject, email);

        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
        try {

            MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
            if (type.equals("editor")) {
                helper.setFrom(userFrom, "Grizzly Editor");
            } else {
                helper.setFrom(userFrom, personal);
            }
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);

            message.setContent(content, "text/html; charset=utf-8");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("could not send email", e);
        }
        return false;
    }

    public boolean sendWithAttachement(String content, String subject, String email, String type, File file) {
        log.info("send email with subject: {} to email: {}", subject, email);

        MimeMessage message = mailSender.createMimeMessage();
        try {

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            if (type.equals("editor")) {
                helper.setFrom(userFrom, "Grizzly Editor");
            } else {
                helper.setFrom(userFrom, personal);
            }
            helper.setTo(email);
            helper.setSubject(subject);

            FileSystemResource res = new FileSystemResource(file);
            helper.addAttachment(file.getName(), res);
            helper.setText(content, true);
            message.setContent(content, "text/html; charset=utf-8");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("could not send email", e);
        }
        return false;
    }

    public String getOutputContent(String templateFileName, String variablesPath, String lang,
                                   ShareContainerDto shareContainerDto) throws IOException {
        Context context = new Context();
        // Choose Language
        if (lang.contains("fr")) {
            variablesPath += "-fr.json";
        } else {
            variablesPath += "-en.json";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = new ObjectMapper()
                .readValue(resourceloader.getResource("classpath:" + variablesPath).getInputStream(), HashMap.class);

        // Set THYMELEAF Variables from JSON File
        context.setVariables(variables);
        context.setVariable("microservice", shareContainerDto.getMicroservice());
        context.setVariable("swagger", shareContainerDto.getSwagger());
        context.setVariable("openAPI", shareContainerDto.getOpenAPI());
        context.setVariable("projectName", shareContainerDto.getProjectName());
        return templateEngine.process(templateFileName, context);
    }


    public String getOutputDockerMailContent(String templateFileName, String variablesPath, String lang,
                                             DockerExport dockerExport) throws IOException {
        Context context = new Context();
        // Choose Language
        if (lang.contains("fr")) {
            variablesPath += "-fr.json";
        } else {
            variablesPath += "-en.json";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = new ObjectMapper()
                .readValue(resourceloader.getResource("classpath:" + variablesPath).getInputStream(), HashMap.class);

        // Set THYMELEAF Variables from JSON File
        context.setVariables(variables);
        context.setVariable("mongoUriDescription", "mongoUri: The connection URI");
        context.setVariable("export", dockerExport);
        context.setVariable("repoUrl", "Docker URL: " + dockerExport.getRepoUrl());
        context.setVariable("url", frontUrl + "/app/project/" + dockerExport.getProjectId());
        context.setVariable("dockerStop", "docker stop " + dockerExport.getProjectName());
        context.setVariable("dockerTest", "http://localhost:8050/swagger-ui.html");
        if (dockerExport.getDatabaseType().equals("nosql")) {
            context.setVariable("dockerRun", "docker run -d -p 8050:8050 --env \"mongoUri=MongoDb_Uri\" --env \"mongoDatabase=" + dockerExport.getProjectName().replace(".", "") + "DB" + "\" --name=" + dockerExport.getProjectName() + "_" + dockerExport.getContainerName() + " " + dockerExport.getRepoUrl());
            context.setVariable("dockerFunctionRun", "docker run -d -p 8050:8050 --env \"grizzly-runtime-function-url=<ip-address>:8080\"  --env \"mongoUri=MongoDb_Uri\" --env \"mongoDatabase=" + dockerExport.getProjectName().replace(".", "") + "DB" + "\" --name=" + dockerExport.getProjectName() + "_" + dockerExport.getContainerName() + " " + dockerExport.getRepoUrl());
        }
        if (dockerExport.getDatabaseType().equals("sql")) {
            context.setVariable("dockerRun", "docker run -d -p 8050:8050 --env \"databaseProvider=" + dockerExport.getDatabaseProvider() + "\" --env \"databaseName=" + dockerExport.getDatabaseName() + "\" --env \"databasePort=" + dockerExport.getDatabasePort() + "\" --env \"databaseHost=" + dockerExport.getDatabaseHost() + "\" --env \"databasePassword=" + dockerExport.getDatabasePassword() + "\" --env \"databaseUsername=" + dockerExport.getDatabaseUsername() + "\" --name=" + dockerExport.getProjectName() + "_" + dockerExport.getContainerName() + " " + dockerExport.getRepoUrl());
            context.setVariable("dockerFunctionRun", "docker run -d -p 8050:8050 --env \"grizzly-runtime-function-url=<ip-address>:8080\"  --env \"databaseProvider=" + dockerExport.getDatabaseProvider() + "\" --env \"databaseName=" + dockerExport.getDatabaseName() + "\" --env \"databasePort=" + dockerExport.getDatabasePort() + "\" --env \"databaseHost=" + dockerExport.getDatabaseHost() + "\" --env \"databasePassword=" + dockerExport.getDatabasePassword() + "\" --env \"databaseUsername=" + dockerExport.getDatabaseUsername() + "\" --name=" + dockerExport.getProjectName() + "_" + dockerExport.getContainerName() + " " + dockerExport.getRepoUrl());
        }
        context.setVariable("functionRun", "docker run -d -p 8080:8080 " + functionImageUrl);
        context.setVariable("dockerPull", "docker pull " + dockerExport.getRepoUrl());
        context.setVariable("creationTime", "Creation Time: " + dockerExport.getCreationTime());
        context.setVariable("databaseUriExample", "mongoUri = mongodb://localhost:27017 (required)");
        context.setVariable("containerNameExample", "name = " + dockerExport.getProjectName() + "_" + dockerExport.getContainerName().replace(".", "") + " (optional)");
        context.setVariable("databaseNameExample", dockerExport.getProjectName().replace(".", "") + "DB");
        return templateEngine.process(templateFileName, context);
    }

}

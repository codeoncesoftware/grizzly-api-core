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

import fr.codeonce.grizzly.core.domain.container.Contact;
import fr.codeonce.grizzly.core.domain.container.License;
import fr.codeonce.grizzly.core.domain.container.Server;
import fr.codeonce.grizzly.core.domain.endpointModel.BodyMap;
import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import fr.codeonce.grizzly.core.domain.resource.OpenAPIResponse;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;
import fr.codeonce.grizzly.core.service.resource.utils.ResourceGroupDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContainerDto {

    private String id;

    private String name;

    private String description;

    private Date creationTime;

    private Date lastUpdate;

    private String projectId;

    private List<ResourceGroupDto> resourceGroups = new ArrayList<>();

    private List<String> schemes;

    //private List<Resource> resources = new ArrayList<>();

    private List<EndpointModel> endpointModels = new ArrayList<>();

    private List<ResourceParameter> parameters = new ArrayList<>();

    private boolean enabled;

    private String userEmail;

    private String hierarchy = "";

    private String swaggerUuid;

    private String termsOfService;

    private Contact contact;

    private Boolean firstCreation;

    private License license;

    private List<BodyMap> requestBodies;

    private List<Server> servers;

    private String host;

    private String basePath;

    private List<OpenAPIResponse> responses;

    private String exportPreference;

    private boolean hasFunctions;

    public boolean isHasFunctions() {
        return hasFunctions;
    }

    public void setHasFunctions(boolean hasFunctions) {
        this.hasFunctions = hasFunctions;
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public void setSchemes(List<String> schemes) {
        this.schemes = schemes;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getExportPreference() {
        return exportPreference;
    }

    public void setExportPreference(String exportPreference) {
        this.exportPreference = exportPreference;
    }

    public List<OpenAPIResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<OpenAPIResponse> responses) {
        this.responses = responses;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<BodyMap> getRequestBodies() {
        return requestBodies;
    }

    public void setRequestBodies(List<BodyMap> requestBodies) {
        this.requestBodies = requestBodies;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public List<ResourceGroupDto> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(List<ResourceGroupDto> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSwaggerUuid() {
        return swaggerUuid;
    }

    public void setSwaggerUuid(String swaggerUuid) {
        this.swaggerUuid = swaggerUuid;
    }

    public List<EndpointModel> getEndpointModels() {
        return endpointModels;
    }

    public void setEndpointModels(List<EndpointModel> endpointModels) {
        this.endpointModels = endpointModels;
    }

    public List<ResourceParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ResourceParameter> parameters) {
        this.parameters = parameters;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public Boolean getFirstCreation() {
        return firstCreation;
    }

    public void setFirstCreation(Boolean firstCreation) {
        this.firstCreation = firstCreation;
    }

}

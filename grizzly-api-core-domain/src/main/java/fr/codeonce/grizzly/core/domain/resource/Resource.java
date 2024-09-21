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
package fr.codeonce.grizzly.core.domain.resource;

import fr.codeonce.grizzly.common.runtime.resource.MicrogatewayMapping;
import fr.codeonce.grizzly.core.domain.endpointModel.BodyMap;

import java.util.ArrayList;
import java.util.List;

public class Resource {

    private String name;
    private String summary;
    private String description;
    private ResourceFile resourceFile;
    private List<ResourceFile> secondaryFilePaths;
    private String path;
    private String httpMethod;
    // FIXME USE ENUM OF STRING
    private String executionType;
    private List<String> inFunctions = new ArrayList<>();
    private List<String> outFunctions = new ArrayList<>();
    private List<String> functions = new ArrayList<>();
    private CustomQuery customQuery;
    private List<String> consumes;
    private List<String> produces;
    private String resourceGroup;
    private List<ResourceParameter> parameters;
    private List<APIResponse> responses;
    private List<OpenAPIResponse> openAPIResponses;
    private BodyMap requestBody;
    private ArrayList<String> securityLevel = new ArrayList<>();
    private List<String> fields;
    private boolean pageable;
    private List<String> missingAttributes = new ArrayList<>();
    private String serviceURL;
    private List<MicrogatewayMapping> mapping;
    private List<ResourceRequestModel> requestModels;
    private ResourceLog resourceLog;

    public ResourceLog getResourceLog() {
        return resourceLog;
    }

    public void setResourceLog(ResourceLog resourceLog) {
        this.resourceLog = resourceLog;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }

    public List<ResourceRequestModel> getRequestModels() {
        return requestModels;
    }

    public void setRequestModels(List<ResourceRequestModel> requestModal) {
        this.requestModels = requestModal;
    }

    public List<MicrogatewayMapping> getMapping() {
        return mapping;
    }

    public void setMapping(List<MicrogatewayMapping> mapping) {
        this.mapping = mapping;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public BodyMap getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(BodyMap requestBody) {
        this.requestBody = requestBody;
    }

    public List<OpenAPIResponse> getOpenAPIResponses() {
        return openAPIResponses;
    }

    public void setOpenAPIResponses(List<OpenAPIResponse> openAPIResponses) {
        this.openAPIResponses = openAPIResponses;
    }

    public List<String> getMissingAttributes() {
        return missingAttributes;
    }

    public void setMissingAttributes(List<String> missingAttributes) {
        this.missingAttributes = missingAttributes;
    }

    public Resource() {
        this.secondaryFilePaths = new ArrayList<>();
        this.customQuery = new CustomQuery();
        this.consumes = new ArrayList<>();
        this.produces = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.responses = new ArrayList<>();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ResourceParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ResourceParameter> parameters) {
        this.parameters = parameters;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public CustomQuery getCustomQuery() {
        return customQuery;
    }

    public void setCustomQuery(CustomQuery customQuery) {
        this.customQuery = customQuery;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public ResourceFile getResourceFile() {
        return resourceFile;
    }

    public void setResourceFile(ResourceFile resourceFile) {
        this.resourceFile = resourceFile;
    }

    public List<APIResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<APIResponse> responses) {
        this.responses = responses;
    }

    public List<ResourceFile> getSecondaryFilePaths() {
        return secondaryFilePaths;
    }

    public void setSecondaryFilePaths(List<ResourceFile> secondaryFilePaths) {
        this.secondaryFilePaths = secondaryFilePaths;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public boolean isPageable() {
        return pageable;
    }

    public void setPageable(boolean pageable) {
        this.pageable = pageable;
    }

    public List<String> getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(List<String> securityLevel) {
        this.securityLevel = (ArrayList<String>) securityLevel;
    }

    public List<String> getInFunctions() {
        return inFunctions;
    }

    public void setInFunctions(List<String> inFunctions) {
        this.inFunctions = inFunctions;
    }

    public List<String> getOutFunctions() {
        return outFunctions;
    }

    public void setOutFunctions(List<String> outFunctions) {
        this.outFunctions = outFunctions;
    }

}

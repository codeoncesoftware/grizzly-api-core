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
package fr.codeonce.grizzly.core.service.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.codeonce.grizzly.common.runtime.resource.MicrogatewayMapping;
import fr.codeonce.grizzly.core.domain.endpointModel.BodyMap;
import fr.codeonce.grizzly.core.domain.resource.OpenAPIResponse;
import fr.codeonce.grizzly.core.domain.resource.ResourceLog;
import fr.codeonce.grizzly.core.domain.resource.ResourceRequestModel;
import fr.codeonce.grizzly.core.service.datasource.query.CustomQueryDto;
import fr.codeonce.grizzly.core.service.resource.utils.APIResponseDTO;
import fr.codeonce.grizzly.core.service.resource.utils.ResourceFileDto;
import fr.codeonce.grizzly.core.service.resource.utils.ResourceParameterDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceDto {

    private String name;
    private String summary;
    private String description;
    private ResourceFileDto resourceFile;
    private List<ResourceFileDto> secondaryFilePaths;
    private String path;
    private String httpMethod;
    private String executionType;
    private CustomQueryDto customQuery;
    private List<String> inFunctions;
    private List<String> outFunctions;
    private List<String> functions;
    // This Fields gets populated at Mapping
    Map<String, List<String>> databases;
    private List<String> consumes;
    private List<String> produces;
    private List<APIResponseDTO> responses;
    private List<String> fields;
    private boolean pageable;
    private List<ResourceParameterDto> parameters;
    private ArrayList<String> securityLevel = new ArrayList<>();
    private List<String> missingAttributes;
    private List<OpenAPIResponse> openAPIResponses;
    private List<ResourceRequestModel> resourceModels;

    private String serviceURL;
    private List<MicrogatewayMapping> mapping;
    private ResourceLog resourceLog;

    public ResourceLog getResourceLog() {
        return resourceLog;
    }

    public void setResourceLog(ResourceLog resourceLogs) {
        this.resourceLog = resourceLogs;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }

    public List<MicrogatewayMapping> getMapping() {
        return mapping;
    }

    public void setMapping(List<MicrogatewayMapping> mapping) {
        this.mapping = mapping;
    }

    public void setResourceModels(List<ResourceRequestModel> resourceModels) {
        this.resourceModels = resourceModels;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    private BodyMap requestBody;

    public BodyMap getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(BodyMap requestBody) {
        this.requestBody = requestBody;
    }

    public List<String> getMissingAttributes() {
        return missingAttributes;
    }

    public void setMissingAttributes(List<String> missingAttributes) {
        this.missingAttributes = missingAttributes;
    }

    public ResourceDto() {
        resourceFile = new ResourceFileDto();
        secondaryFilePaths = new ArrayList<>();
        customQuery = new CustomQueryDto();
        consumes = new ArrayList<>();
        produces = new ArrayList<>();
        responses = new ArrayList<>();
    }

    public List<OpenAPIResponse> getOpenAPIResponses() {
        return openAPIResponses;
    }

    public void setOpenAPIResponses(List<OpenAPIResponse> openAPIResponses) {
        this.openAPIResponses = openAPIResponses;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ResourceParameterDto> getParameters() {
        return parameters;
    }

    public void setParameters(List<ResourceParameterDto> parameters) {
        this.parameters = parameters;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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

    public CustomQueryDto getCustomQuery() {
        return customQuery;
    }

    public void setCustomQuery(CustomQueryDto customQuery) {
        this.customQuery = customQuery;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
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

    public List<APIResponseDTO> getResponses() {
        return responses;
    }

    public void setResponses(List<APIResponseDTO> responses) {
        this.responses = responses;
    }

    public List<ResourceFileDto> getSecondaryFilePaths() {
        return secondaryFilePaths;
    }

    public void setSecondaryFilePaths(List<ResourceFileDto> secondaryFilePaths) {
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

    public ResourceFileDto getResourceFile() {
        return resourceFile;
    }

    public void setResourceFile(ResourceFileDto resourceFile) {
        this.resourceFile = resourceFile;
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

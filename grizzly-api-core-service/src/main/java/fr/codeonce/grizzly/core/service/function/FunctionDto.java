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
package fr.codeonce.grizzly.core.service.function;

import fr.codeonce.grizzly.common.runtime.AWSCredentials;

import java.util.Date;
import java.util.Map;

public class FunctionDto {

    private String id;

    private String language;

    private String function;

    private String logs;

    private String name;

    private String projectId;

    private String version;

    private Date creationTime;

    private Date lastUpdate;

    // java
    private String className;

    private String methodName;

    private String model;

    private String modelName;

    private AWSCredentials awsCredentials;

    private String awsFunctionName;

    private String OpenFaasBody;

    private String OpenFaasURI;

    private Map<String, Object> OpenFaasHeaders;


    public String getOpenFaasBody() {
        return OpenFaasBody;
    }

    public void setOpenFaasBody(String openFaasBody) {
        OpenFaasBody = openFaasBody;
    }

    public String getOpenFaasURI() {
        return OpenFaasURI;
    }

    public void setOpenFaasURI(String openFaasURI) {
        OpenFaasURI = openFaasURI;
    }

    public Map<String, Object> getOpenFaasHeaders() {
        return OpenFaasHeaders;
    }

    public void setOpenFaasHeaders(Map<String, Object> openFaasHeaders) {
        OpenFaasHeaders = openFaasHeaders;
    }

    /**
     * @return the awsFunctionName
     */
    public String getAwsFunctionName() {
        return awsFunctionName;
    }

    /**
     * @param awsFunctionName the awsFunctionName to set
     */
    public void setAwsFunctionName(String awsFunctionName) {
        this.awsFunctionName = awsFunctionName;
    }

    /**
     * @return the awsCredentials
     */
    public AWSCredentials getAwsCredentials() {
        return awsCredentials;
    }

    /**
     * @param awsCredentials the awsCredentials to set
     */
    public void setAwsCredentials(AWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String langugage) {
        this.language = langugage;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

}

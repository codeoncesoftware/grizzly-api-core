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
package fr.codeonce.grizzly.core.service.datasource;

import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.datasource.CustomDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

;

public class DBSourceDto {

    protected String id;

    protected String name;

    protected String description;

    protected String uri;

    protected String database;

    protected String userEmail;

    protected String username;

    protected char[] password;

    protected Date creationTime;

    protected Date lastUpdate;

    protected boolean active;

    protected String connectionMode;

    private String host;

    private Integer port;

    private String physicalDatabase;

    private String authenticationDatabase;

    private String gridFsDatabase;

    protected Provider provider;

    private String bucketName;

    private String clusterName;

    private List<CustomDatabase> databases = new ArrayList<>();

    private boolean secured;

    private List<String> teamIds = new ArrayList<>();

    private String organizationId;

    private String type;

    private boolean dockerMode = false;

    private boolean authDBEnabled;

    protected boolean connectionSucceeded;

    public boolean isDockerMode() {
        return dockerMode;
    }

    public void setDockerMode(boolean dockerMode) {
        this.dockerMode = dockerMode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public List<String> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
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

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPhysicalDatabase() {
        return physicalDatabase;
    }

    public void setPhysicalDatabase(String physicalDatabase) {
        this.physicalDatabase = physicalDatabase;
    }

    public String getAuthenticationDatabase() {
        return authenticationDatabase;
    }

    public void setAuthenticationDatabase(String authenticationDatabase) {
        this.authenticationDatabase = authenticationDatabase;
    }

    public String getGridFsDatabase() {
        return gridFsDatabase;
    }

    public void setGridFsDatabase(String gridFsDatabase) {
        this.gridFsDatabase = gridFsDatabase;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<CustomDatabase> getDatabases() {
        return databases;
    }

    public void setDatabases(List<CustomDatabase> list) {
        this.databases = list;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean isAuthDBEnabled() {
        return authDBEnabled;
    }

    public void setAuthDBEnabled(boolean authDBEnabled) {
        this.authDBEnabled = authDBEnabled;
    }

    public boolean isConnectionSucceeded() {
        return connectionSucceeded;
    }

    public void setConnectionSucceeded(boolean connectionSucceeded) {
        this.connectionSucceeded = connectionSucceeded;
    }
}

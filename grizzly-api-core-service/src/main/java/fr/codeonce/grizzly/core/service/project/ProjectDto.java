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
package fr.codeonce.grizzly.core.service.project;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ProjectDto {

    private String id;

    private String name;

    private Date creationTime;

    private Date lastUpdate;

    private String description;

    private String userEmail;

    private String dbsourceId;

    private List<String> IdentityProviderIds;
    private String databaseName;

    private String type;

    private SecurityApiConfigDto securityConfig;

    private List<String> roles = new ArrayList<>();

    private List<String> teamIds = new ArrayList<>();

    private String runtimeUrl;

    private String organizationId;

    private String gitUrl;

    private String gitUsername;

    private String gitPassword;

    private String gitToken;

    private String gitLocalpath;

    private String gitBranch;

    private boolean used;

    private boolean securityEnabled;

    private boolean userManagementEnabled;

    private String authMSRuntimeURL;

    private List<SecurityApiConfigDto> authorizedApps;

    private String iamDelegatedSecurity;

    public List<SecurityApiConfigDto> getAuthorizedApps() {
        return authorizedApps;
    }

    public void setAuthorizedApps(List<SecurityApiConfigDto> authorizedApps) {
        this.authorizedApps = authorizedApps;
    }

    public String getAuthMSRuntimeURL() {
        return authMSRuntimeURL;
    }

    public void setAuthMSRuntimeURL(String authMSRuntimeURL) {
        this.authMSRuntimeURL = authMSRuntimeURL;
    }

    public List<String> getIdentityProviderIds() {
        return IdentityProviderIds;
    }

    public void setIdentityProviderIds(List<String> identityProviderIds) {
        IdentityProviderIds = identityProviderIds;
    }

    public boolean isUserManagementEnabled() {
        return userManagementEnabled;
    }

    public void setUserManagementEnabled(boolean userManagementEnabled) {
        this.userManagementEnabled = userManagementEnabled;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public void setSecurityEnabled(boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean getUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitUsername() {
        return gitUsername;
    }

    public void setGitUsername(String gitUsername) {
        this.gitUsername = gitUsername;
    }

    public String getGitPassword() {
        return gitPassword;
    }

    public void setGitPassword(String gitPassword) {
        this.gitPassword = gitPassword;
    }

    public String getGitToken() {
        return gitToken;
    }

    public void setGitToken(String gitToken) {
        this.gitToken = gitToken;
    }

    public String getGitLocalpath() {
        return gitLocalpath;
    }

    public void setGitLocalpath(String gitLocalpath) {
        this.gitLocalpath = gitLocalpath;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getId() {
        return id;
    }

    public String getRuntimeUrl() {
        return runtimeUrl;
    }

    public void setRuntimeUrl(String runtimeUrl) {
        this.runtimeUrl = runtimeUrl;
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

    public Date getCreationTime() {
        return creationTime;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDbsourceId() {
        return dbsourceId;
    }

    public void setDbsourceId(String dbsourceId) {
        this.dbsourceId = dbsourceId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    public SecurityApiConfigDto getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityApiConfigDto securityConfig) {
        this.securityConfig = securityConfig;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getIamDelegatedSecurity() {
        return iamDelegatedSecurity;
    }

    public void setIamDelegatedSecurity(String iamDelegatedSecurity) {
        this.iamDelegatedSecurity = iamDelegatedSecurity;
    }


}

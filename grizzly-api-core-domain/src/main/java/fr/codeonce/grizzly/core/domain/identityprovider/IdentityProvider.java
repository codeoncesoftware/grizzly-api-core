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
package fr.codeonce.grizzly.core.domain.identityprovider;

import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.common.runtime.KeycloakCredentials;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document("identityprovider")
public class IdentityProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private IdentityProviders name;

    private String description;

    private String displayedName;

    private String userEmail;

    private String username;

    private KeycloakCredentials credentials;

    private List<String> teamIds = new ArrayList<>();

    private String organizationId;

    private Boolean connectionSucceeded;

    public Boolean getConnectionSucceeded() {
        return connectionSucceeded;
    }

    public void setConnectionSucceeded(Boolean connectionSucceeded) {
        this.connectionSucceeded = connectionSucceeded;
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

    public KeycloakCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(KeycloakCredentials credentials) {
        this.credentials = credentials;
    }

    public String getDisplayedName() {
        return displayedName;
    }

    public void setDisplayedName(String displayedName) {
        this.displayedName = displayedName;
    }

    @CreatedDate
    private Date creationTime;

    @LastModifiedDate
    private Date lastUpdate;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public IdentityProviders getName() {
        return name;
    }

    public void setName(IdentityProviders name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

}
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
package fr.codeonce.grizzly.core.domain.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.util.Collections;

@ConditionalOnProperty(value = "docker", havingValue = "false")
@Configuration
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    @Autowired
    private MongoProperties mongoProperties;

    @Override
    @Bean
    public MongoClient mongoClient() {

        // URI Connection
        if (mongoProperties.getUri() != null) {
            return MongoClients.create(mongoProperties.getUri());
        }
        // REMOTE SERVER
        // SERVER INFO
        ServerAddress serverAddress = new ServerAddress(mongoProperties.getHost(), mongoProperties.getPort());
        if (StringUtils.isNotBlank(mongoProperties.getUsername())) {
            // CREDENTIAL
            MongoCredential credential = MongoCredential.createCredential(mongoProperties.getUsername(),
                    mongoProperties.getAuthenticationDatabase(), mongoProperties.getPassword());

            MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
                sett.hosts(Collections.singletonList(serverAddress));
            }).credential(credential).build();

            // CREATE MONGO CLIENT
            return MongoClients.create(settings);
        }

        MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
            sett.hosts(Collections.singletonList(serverAddress));
        }).build();
        // LOCAL HOST
        return MongoClients.create(settings);

    }

    @Override
    protected String getDatabaseName() {
        return mongoProperties.getDatabase();
    }

//	@Bean
//	public GridFsTemplate gridFsTemplate() throws Exception {
//		return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter(null, null, null));
//	}

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}

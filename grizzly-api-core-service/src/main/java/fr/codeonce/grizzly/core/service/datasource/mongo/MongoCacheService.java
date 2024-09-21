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
package fr.codeonce.grizzly.core.service.datasource.mongo;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class MongoCacheService {

    private static final String FREE = "FREE";

    private static final String CLOUD = "CLOUD";

    @Autowired
    private MongoDBSourceMapperService dbsourceMapper;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    @Qualifier("coreCryptoHelper")
    private CryptoHelper encryption;

    @Autowired
    @Qualifier("atlasMongoClient")
    private MongoClient atlasMongoClient;


    @Cacheable(value = "mongoClients", key = "#dbsourceId")
    public MongoClient getMongoClient(String dbsourceId) {
        return getClient(dbsourceId);
    }

    public MongoClient getTemporaryMClient(DBSourceDto dbsourceDto) {
        DBSource dbsource = this.dbsourceMapper.mapToDomain(dbsourceDto);
        encryption.decrypt(dbsource);
        return this.prepareMongoClient(dbsource);
    }

    public MongoClient getAtlasMongoClient() {
        return atlasMongoClient;
    }

    @CachePut(value = "mongoClients", key = "#dbsource.id")
    public MongoClient getUpdatedMongoClient(DBSource dbsource) {
        return this.prepareMongoClient(dbsource);
    }

    @Cacheable(value = "mongoClients", key = "#dbsource.id")
    public MongoClient getMongoClient(DBSource dbsource) {
        return this.prepareMongoClient(dbsource);

    }

    @Cacheable(value = "gridFsObjects")
    public GridFsTemplate getGridFs(MongoClient mongoClient, String databaseName) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
        return new GridFsTemplate(mongoTemplate.getMongoDatabaseFactory(), mongoTemplate.getConverter());
    }

    @CacheEvict(value = "gridFsObjects")
    public void evictGridFs(MongoClient mongoClient, String databaseName) {
        // Empty GridFsTemplate Instance From Cache
    }

    @CacheEvict(value = "mongoClients", key = "#dbsourceId")
    public void evictMongoClient(String dbsourceId) {
        // Empty MongoClient Instance From Cache
    }

    private MongoClient prepareMongoClient(DBSource dbsource) {
        if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
            return getAtlasMongoClient();
        }
        // IF AN URI IS PRESENT
        if (dbsource.getConnectionMode().equalsIgnoreCase(CLOUD) && !dbsource.getUri().isBlank()) {
            try {
                ConnectionString uri = new ConnectionString(dbsource.getUri());
                MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
                    sett.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS);

                }).applyToSocketSettings(sett -> {
                    sett.connectTimeout(60000, TimeUnit.MILLISECONDS);
                }).applyConnectionString(uri).build();
                return MongoClients.create(settings);
            } catch (RuntimeException e) {
                return null;
            }


        } else {
            // SERVER INFO
            try {
                ServerAddress serverAddress;
                serverAddress = new ServerAddress(dbsource.getHost(), dbsource.getPort());
                // REMOTE SERVER
                if (dbsource.isSecured()) {
                    // CREDENTIAL
                    MongoCredential credential = MongoCredential.createCredential(dbsource.getUsername(),
                            dbsource.getAuthenticationDatabase(), dbsource.getPassword());

                    MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
                        sett.hosts(Collections.singletonList(serverAddress));
                        sett.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS);
                    }).applyToSocketSettings(sett -> {
                        sett.connectTimeout(60000, TimeUnit.MILLISECONDS);
                    }).credential(credential).build();

                    // CREATE MONGO CLIENT
                    return MongoClients.create(settings);
                } else {

                    MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
                        sett.hosts(Collections.singletonList(serverAddress));
                        sett.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS);
                    }).applyToSocketSettings(sett -> {
                        sett.connectTimeout(60000, TimeUnit.MILLISECONDS);
                    }).build();
                    return MongoClients.create(settings);
                }
            } catch (MongoSocketException e) {
                return null;
            }
        }
    }

    @CachePut(value = "mongoClients", key = "#dbsourceID")
    public MongoClient updateCache(String dbsourceID) {
        return getClient(dbsourceID);
    }

    private MongoClient getClient(String dbsourceId) {
        return this.dbSourceRepository.findById(dbsourceId).map(d -> {
            encryption.decrypt(d);
            if (d.getConnectionMode().equalsIgnoreCase(FREE)) {
                return getAtlasMongoClient();
            } else {
                return prepareMongoClient(d);
            }
        }).orElseThrow();
    }
}

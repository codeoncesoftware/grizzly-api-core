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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MongoDBSourceStatsService {

    private static final Logger log = LoggerFactory.getLogger(MongoDBSourceStatsService.class);

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private MongoCacheService mongoCacheService;

    public Document getCollectionStats(DBSource dbSource, String databaseName, String collectionName) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("collStats", collectionName);
        options.put("scale", 1024);
        return getStats(dbSource, databaseName, new Document(options));
    }

    public Set<String> getCollectionAttributes(DBSource dbSource, String databaseName, String collectionName) {
        MongoClient mClient = mongoCacheService.getMongoClient(dbSource);
        if (dbSource.getConnectionMode().equalsIgnoreCase("FREE")) {
            dbSource.setDatabase(dbSource.getPhysicalDatabase());
        } else {
            dbSource.setDatabase(databaseName);
        }
        try {
            MongoDatabase database = mClient.getDatabase(dbSource.getDatabase());
            return database.getCollection(collectionName).find().first().keySet();
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    public Document getCollectionStats(String dbsourceId, String databaseName, String collectionName) {
        return dbSourceRepository.findById(dbsourceId).map(d -> getCollectionStats(d, databaseName, collectionName))//
                .orElseThrow();
    }

    public Document getDbStats(String dbsourceId, String databaseName) {
        return dbSourceRepository.findById(dbsourceId)
                .filter(db -> String.valueOf(db.provider).equals(String.valueOf(Provider.MONGO)))
                .map(d -> getDbStats(d, databaseName))//
                .filter(Objects::nonNull)
                .orElseThrow();
    }

    public Document getDbStats(DBSource dbsource, String databaseName) {
        Map<String, Object> options = new HashMap<>();
        options.put("dbStats", 1);
        options.put("scale", 1024);
        return getStats(dbsource, databaseName, new Document(options));
    }

    private Document getStats(DBSource dbsource, String databaseName, Document command) {

        try {
            MongoClient mClient = mongoCacheService.getMongoClient(dbsource);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase("FREE")) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                return database.runCommand(command);
            } else {
                return new Document("dataSize", 0);
            }
        } catch (Exception e) {
            log.warn("could not get stats", e);
            return new Document("dataSize", 0);
        }
    }

}

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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import fr.codeonce.grizzly.core.domain.datasource.CustomDatabase;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.IDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MongoDBSourceService implements IDBSourceService {

    private static final String FREE = "FREE";

    private static final String CLOUD = "CLOUD";

    private static final String ON_PREMISE = "ON-PREMISE";

    private static final String AUTHENTICATION_USER = "authentication_user";

    private static final String ADMIN = "admin";

    private static final String DEMO = "demo";


    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MongoProperties mongoProperties;

    @Autowired
    @Qualifier("atlasMongoClient")
    private MongoClient mongoClient;

    @Autowired
    private MongoDBSourceMapperService mapper;

    @Autowired
    private MongoCacheService cacheService;

    @Autowired
    @Qualifier("coreRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("coreCryptoHelper")
    private CryptoHelper encryption;

    @Value("${frontUrl}")
    private String url;

    private static final Logger log = LoggerFactory.getLogger(MongoDBSourceService.class);

    /**
     * Save a DBSource Object in DataBase
     *
     * @param dbSource
     * @return a mapped Instance of DBSource
     */
    public DBSourceDto saveDBSource(DBSourceDto dbSourceDto) {
        String currentUserEmail = "";
        if (SecurityContextUtil.getCurrentUserEmail() != null) {
            currentUserEmail = SecurityContextUtil.getCurrentUserEmail();
            if (!currentUserEmail.contains("@")) {
                User u = userService.getConnectedUserByApiKey();
                currentUserEmail = u.getEmail();
            }
        } else if (dbSourceDto.getUserEmail() != null) {
            currentUserEmail = dbSourceDto.getUserEmail();
        } else {
            currentUserEmail = "editor@codeonce.fr";
        }

        User user = userRepository.findByEmail(currentUserEmail).orElse(null);
        MongoClient mClient;
        StopWatch watch = new StopWatch();
        watch.start("saving dbsource");

        boolean isFree = dbSourceDto.getConnectionMode().equalsIgnoreCase(FREE);

        if (dbSourceDto.getConnectionMode().equalsIgnoreCase(CLOUD)) {
            String dbName = dbSourceDto.getUri().substring(dbSourceDto.getUri().lastIndexOf('/'));
            dbName = dbName.substring(1);
            dbSourceDto.setDatabase(dbName);
        }

        DBSource newDbSource = mapper.mapToDomain(dbSourceDto);
        newDbSource.setUserEmail(currentUserEmail);
        if (newDbSource.getId() != null) {
            encryption.decrypt(newDbSource);
        }

        // First Registration
        if (newDbSource.getId() == null && isFree) {
            if (user == null) {
                newDbSource.setPhysicalDatabase(currentUserEmail.substring(0, 4) + '_'
                        + newDbSource.getDatabase().replaceAll("-", "").replaceAll("_", "") + '_'
                        + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15));
            } else {
                newDbSource.setPhysicalDatabase(
                        user.getFirstName() + '_' + newDbSource.getDatabase().replaceAll("-", "").replaceAll("_", "") + '_'
                                + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15));
            }

        }

        String databaseName;
        if (newDbSource.getId() != null && isFree) {
            databaseName = this.dbSourceRepository.findById(newDbSource.getId()).map(DBSource::getPhysicalDatabase)
                    .orElseThrow();
            newDbSource.setPhysicalDatabase(databaseName);
        } else {
            databaseName = newDbSource.getDatabase();
        }

        if (dbSourceDto.getConnectionMode().equalsIgnoreCase(ON_PREMISE)) {
            if (dbSourceDto.getHost() == null || dbSourceDto.getPort() == null) {
                throw new IllegalArgumentException("4014");
            }
            newDbSource.setDatabase(cacheService.getTemporaryMClient(dbSourceDto).listDatabaseNames().first());
            if (databaseName == null) {
                newDbSource.setDatabase("test");
            }
        }
        if (dbSourceDto.getConnectionMode().equalsIgnoreCase(CLOUD) && dbSourceDto.getUri() == null) {
            throw new IllegalArgumentException("4015");
        }

        encryption.encrypt(newDbSource);
        newDbSource = this.dbSourceRepository.save(newDbSource);
        encryption.decrypt(newDbSource);

        if (isFree) {
            databaseName = newDbSource.getPhysicalDatabase();
            mClient = this.cacheService.getAtlasMongoClient();
        } else {
            mClient = getOnPremiseMClient(newDbSource);
        }
        // Create first collections if it's a Free DB
        if (isFree && !dbSourceDto.isDockerMode() && dbSourceDto.isAuthDBEnabled()) {
            addFirstCollections(mClient, databaseName, AUTHENTICATION_USER);
        }

        watch.stop();
        log.info(watch.prettyPrint());

        updateCacheInstance(newDbSource.getId());

        return mapper.mapToDto(newDbSource);
    }


    /**
     * Updates the Client instance in cache for Core and Runtime
     *
     * @param dbsourceId
     */
    private void updateCacheInstance(String dbsourceId) {
        this.cacheService.updateCache(dbsourceId);
    }

    public MongoClient getOnPremiseMClient(DBSource newDbSource) {
        MongoClient mClient;
        if (newDbSource.getId() != null) {
            mClient = this.cacheService.getMongoClient(newDbSource);
        } else {
            mClient = this.cacheService.getUpdatedMongoClient(newDbSource);
        }
        return mClient;
    }

    @Async
    private void setConnectivitystatus(DBSourceDto dbSourceDto, MongoClient mClient) {
        boolean status = checkConnection(mClient);
        dbSourceDto.setActive(status);
    }

    public DBSource getDBSource(String dbsourceName) {

        List<DBSource> dbSourcesList = dbSourceRepository.findByUserEmailAndName(getCurrentUserEmail(), dbsourceName);
        if (dbSourcesList.size() != 1) {
            return null;
        } else {
            DBSource db = dbSourcesList.get(0);
            encryption.decrypt(db);
            return db;
        }
    }

    /**
     * Check Whether the given Data source DTO Details are Correct or Not
     *
     * @param dbSourceDto
     * @return true on Success, false on Failure
     */
    public boolean checkConnection(DBSourceDto dbSourceDto) {
        MongoClient mClient;
        if (dbSourceDto.getConnectionMode().equalsIgnoreCase(FREE)) {
            mClient = this.cacheService.getAtlasMongoClient();
        } else {
            mClient = cacheService.getMongoClient(mapper.mapToDomain(dbSourceDto));
        }
        return checkConnection(mClient);
    }

    public boolean checkTempConnection(DBSourceDto dbSourceDto) {
        try {
            MongoClient mClient = cacheService.getTemporaryMClient(dbSourceDto);
            return checkConnection(mClient);
        } catch (Exception e) {
            log.error("connection exception {}", e);
            return false;
        }
    }

    /**
     * Check Whether the given Data source Details are Correct or Not
     *
     * @param dbSourceDto
     * @return true on Success, false on Failure
     */
    public boolean checkConnection(MongoClient mClient) {
        try {
            MongoIterable<String> databasesList = mongoClient.listDatabaseNames();
            if (mClient != null) {
                MongoDatabase database = mClient.getDatabase(databasesList.first());
                Document res = database.runCommand(new Document("ping", "1"));
                if (res.get("ok").equals(Double.valueOf("1.0")) || res.get("ok").equals(Integer.valueOf("1"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("connection exception {}", e);
            throw new IllegalArgumentException("4061");
        }
        return false;
    }

    /**
     * Fetch All the Data sources from the Database
     *
     * @return A List of mapped Data sources
     */
    public List<DBSourceDto> getAll() {
        return dbSourceRepository.findAllByUserEmail(getCurrentUserEmail()).parallelStream().map(db -> {
            try {
                encryption.decrypt(db);
            } catch (Exception e) {
                log.debug("Error while Decrypting : {}", e.getMessage());
            }
            return mapper.mapToDto(db);
        }).collect(Collectors.toList());
    }

    /**
     * Delete a specific DataSource from the Database based on the Given ID
     *
     * @param dbsourceId : Id of the Data source to Delete
     */
    @Async
    public void deleteByID(String dbsourceId) {
        StopWatch watch = new StopWatch();
        watch.start("Deleting DataSource with id : " + dbsourceId);

        try {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, mongoProperties.getDatabase());
            BasicQuery idQuery = new BasicQuery("{_id:\"" + dbsourceId + "\"}");
            // Delete Data Source From DB
            DBSource db = mongoTemplate.findAndRemove(idQuery, DBSource.class);
            if (db.getConnectionMode().equalsIgnoreCase(FREE)) {
                // If the DB is FREE, Drop It On Delete
                dropDB(db);
            }
            // Update the Related Projects
            Update update = new Update();
            update.unset("dbsourceId");
            update.unset("databaseName");
            BasicQuery query = new BasicQuery("{dbsourceId:\"" + dbsourceId + "\"}");
            this.projectRepository.findByDbsourceId(dbsourceId).parallelStream()
                    .forEach(project -> mongoTemplate.findAndModify(query, update, Project.class));
        } catch (Exception e) {
            log.debug("Database with id : {}  can't be deleted or does not exist", dbsourceId);
        } finally {
            watch.stop();
            log.info(watch.prettyPrint());
        }

    }

    /**
     * Drop the Database if it is FREE
     *
     * @param db
     */
    private void dropDB(DBSource db) {
        cacheService.getAtlasMongoClient().getDatabase(db.getPhysicalDatabase()).drop();
    }

    /**
     * Fetch a DBSource from DB based on the given Id and Return his Mapped DTO
     *
     * @param dbsourceId
     * @return DBSourceDto
     */
    public DBSourceDto getDbSourceDtoById(String dbsourceId) {
        return this.dbSourceRepository.findById(dbsourceId).map(db -> {
            encryption.decrypt(db);
            return mapper.mapToDto(db);
        }).orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, dbsourceId));
    }

    /**
     * Fetch a DBSource from DB based on the given Id
     *
     * @param dbsourceId
     * @return DBSourceDto
     */
    @Cacheable(value = "dbsources", key = "#dbsourceId")
    public DBSource getDbSourceById(String dbsourceId) {
        return this.dbSourceRepository.findById(dbsourceId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, dbsourceId));
    }

    /**
     * @param dto
     * @return
     * @Deprecated Fetch the List of all Collections within a given MongoDB
     */
    public List<String> getDBCollectionsList(MongoClient mongoClient, String databaseName) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
        return mongoTemplate.getCollectionNames().stream()
                .filter(coll -> (!coll.equalsIgnoreCase("fs.chunks") && !coll.equalsIgnoreCase("fs.files")))
                .collect(Collectors.toList());
    }

    public List<CustomDatabase> getDBdatabasesList(DBSourceDto dto) {

        List<CustomDatabase> databasesList = new ArrayList<>();
        MongoClient mClient;

        if (dto.getConnectionMode().equalsIgnoreCase(FREE)) {
            mClient = this.cacheService.getAtlasMongoClient();
            this.dbSourceRepository.findById(dto.getId())
                    .map(dbsource -> databasesList.add(new CustomDatabase(dbsource.getDatabase(),
                            getDBCollectionsList(mClient, dbsource.getPhysicalDatabase()))))
                    .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, dto.getId()));
            return databasesList;
        } else {

            mClient = cacheService.getMongoClient(mapper.mapToDomain(dto));
            if (mClient != null) {
                Spliterator<String> spitr = mClient.listDatabaseNames().spliterator();
                spitr.forEachRemaining(db -> {
                    // Filter MONGO System Databases
                    if (!db.equalsIgnoreCase("config") && !db.equalsIgnoreCase("local") && !db.equalsIgnoreCase(ADMIN)) {

                        CustomDatabase customDatabase = new CustomDatabase(db, getDBCollectionsList(mClient, db));
                        databasesList.add(customDatabase);
                    }
                });
            }
            return databasesList;
        }
    }

    public void dropCollection(String dbsourceId, String databaseName, String collectionName) {
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                database.getCollection(collectionName).drop();
            }
        });
    }

    public void renameCollection(String dbsourceId, String databaseName, String oldCollectionName, String newCollectionName) {
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                database.getCollection(oldCollectionName).renameCollection(new MongoNamespace(database.getName(), newCollectionName));

            }
        });
    }

    public void createCollectionIndex(String dbsourceId, String databaseName, String collectionName, IndexCollection indexRequest) {
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                Map<String, Integer> indexKeysMap = indexRequest.getIndexKeys();

                Document indexKeys = new Document(indexKeysMap);
                IndexOptions indexOptions = indexRequest.getIndexOptions();

                database.getCollection(collectionName).createIndex( indexKeys,  indexOptions);
            }
        });
    }

    public  Set<String> getCollectionFields(String dbsourceId, String databaseName, String collectionName){
        Set<String> fields = new HashSet<>();
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());

                MongoCollection<Document> collection = database.getCollection(collectionName);
                List<Document> documents =  collection.find().into(new ArrayList<>());

                for (Document document : documents) {
                    fields.addAll(document.keySet());
                }
            }
        });
        return fields;
    }


    public boolean addFirstCollections(MongoClient mClient, String databaseName, String collectionName) {

        try {
            MongoDatabase database = mClient.getDatabase(databaseName);
            BasicDBObject options = new BasicDBObject();
            options.put("size", 12121212);
            database.createCollection(collectionName);
            if (collectionName.equals(AUTHENTICATION_USER)) {
                Document document = new Document();
                document.append("firstname", "Administrator");
                document.append("lastname", "Grizzly");
                document.append("username", ADMIN);
                document.append("password", ADMIN);
                document.append("roles", Arrays.asList(ADMIN));
                document.append("enabled", true);
                database.getCollection(AUTHENTICATION_USER).insertOne(document);
            } else if (collectionName.equals(DEMO)) {
                Document document1 = new Document();
                document1.append("username", "JohnDoe");
                document1.append("firstname", "John");
                document1.append("lastname", "Doe");
                Document document2 = new Document();
                document2.append("username", "CodeOnce");
                document2.append("firstname", "Code");
                document2.append("lastname", "Once");
                database.getCollection(DEMO).insertOne(document1);
                database.getCollection(DEMO).insertOne(document2);

            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkUnicity(String name, String dbsourceId) {
        boolean[] exists = new boolean[]{true};
        this.dbSourceRepository.findByNameIgnoreCaseAndUserEmail(name, getCurrentUserEmail()).ifPresent(db -> {
            if (!(!StringUtils.isBlank(dbsourceId) && dbsourceId.equalsIgnoreCase(db.getId()))) {
                exists[0] = false;
            }
        });
        return exists[0];
    }

    public void addNewCollection(DBSource dbsource, String collectionName) {
        MongoClient mClient = cacheService.getMongoClient(dbsource.getId());
        if (mClient != null) {
            String databaseName;
            if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                databaseName = dbsource.getPhysicalDatabase();
            } else {
                databaseName = dbsource.getDatabase();
            }
            MongoDatabase database = mClient.getDatabase(databaseName);
            BasicDBObject options = new BasicDBObject();
            options.put("size", 12121212);
            database.createCollection(collectionName);
        }
    }

    public void deleteCollectionByIndex(String dbsourceId, String databaseName, String collectionName, String indexName) {
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                database.getCollection(collectionName).dropIndex(indexName);
            }
        });
    }

    public List<IndexCollection>  getCollectionIndexes(String dbsourceId, String databaseName, String collectionName) {
       List<IndexCollection> indexRequests=new ArrayList<>();
        this.dbSourceRepository.findById(dbsourceId).ifPresent(dbsource -> {
            encryption.decrypt(dbsource);
            MongoClient mClient = cacheService.getMongoClient(dbsourceId);
            if (mClient != null) {
                if (dbsource.getConnectionMode().equalsIgnoreCase(FREE)) {
                    dbsource.setDatabase(dbsource.getPhysicalDatabase());
                } else {
                    dbsource.setDatabase(databaseName);
                }
                MongoDatabase database = mClient.getDatabase(dbsource.getDatabase());
                ListIndexesIterable<Document> listeIndexes = database.getCollection(collectionName).listIndexes();
                // Utilisation d'un curseur pour itérer et ajouter à la liste
                try (MongoCursor<Document> cursor = listeIndexes.iterator()) {
                    while (cursor.hasNext()) {
                        Document indexDocument = cursor.next();
                        IndexCollection indexRequestTemp = new IndexCollection();

                        // Extraire les clés d'index (généralement dans un champ "key")
                        Document keyDocument = (Document) indexDocument.get("key");

                        Map<String, Integer> indexKeys = new HashMap<>();
                        for (String key : keyDocument.keySet()) {
                            indexKeys.put(key, keyDocument.getInteger(key));
                        }
                        indexRequestTemp.setIndexKeys(indexKeys);

                        // Extraire les options d'index si nécessaire
                        IndexOptions indexOptions = new IndexOptions(); // Créez une instance selon votre logique
                        // Remplissez les options d'index

                        if(indexDocument.getString("name").equals("_id_"))
                            indexOptions.unique(true);
                        else
                            indexOptions.unique(indexDocument.getBoolean("unique",false));

                        indexOptions.name(indexDocument.getString("name"));
                        indexOptions.background(indexDocument.getBoolean("background", false));
                        indexOptions.sparse(indexDocument.getBoolean("sparse", false));
//                        indexOptions.expireAfter(indexDocument.getLong("expireAfterSeconds"));
                        indexOptions.version(indexDocument.getInteger("v", 0));
                        indexOptions.defaultLanguage(indexDocument.getString("default_language"));
                        indexOptions.languageOverride(indexDocument.getString("language_override"));
                        indexOptions.textVersion(indexDocument.getInteger("textIndexVersion", 0));
                        indexOptions.sphereVersion(indexDocument.getInteger("2dsphereIndexVersion", 0));
                        indexOptions.bits(indexDocument.getInteger("bits", 0));
                        indexOptions.min(indexDocument.getDouble("min"));
                        indexOptions.max(indexDocument.getDouble("max"));
                        indexOptions.getVersion();
                        indexOptions.defaultLanguage(indexDocument.getString("default_language"));
                        indexOptions.languageOverride(indexDocument.getString("language_override"));
                        indexOptions.textVersion(indexDocument.getInteger("textIndexVersion", 0));
                        indexOptions.sphereVersion(indexDocument.getInteger("2dsphereIndexVersion", 0));
                        indexOptions.bits(indexDocument.getInteger("bits", 0));
                        indexOptions.min(indexDocument.getDouble("min"));
                        indexOptions.max(indexDocument.getDouble("max"));

                        indexRequestTemp.setIndexOptions(indexOptions);

                        indexRequests.add(indexRequestTemp);
                    }
                }
            }
        });
        return indexRequests;
    }

}

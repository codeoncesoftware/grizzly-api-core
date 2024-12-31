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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.couchdb.CouchDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.couchdb.mapper.CouchDBSourceMapperService;
import fr.codeonce.grizzly.core.service.datasource.elastic.ElasticDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.elastic.mapper.ElasticDBSourceMapperService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.datasource.sql.SqlDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.sql.mapper.SqlDBSourceMapperService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DBSourceService {

    @Autowired
    private MongoDBSourceMapperService mongoMapper;

    @Autowired
    private CouchDBSourceMapperService couchMapper;

    @Autowired
    private ElasticDBSourceMapperService elasticMapper;

    @Autowired
    private SqlDBSourceMapperService sqlMapper;

    @Autowired
    private MongoDBSourceService mongoService;

    @Autowired
    private CouchDBSourceService couchService;

    @Autowired
    private ElasticDBSourceService elasticService;

    @Autowired
    private SqlDBSourceService sqlDBSourceService;

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private CryptoHelper encryption;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberRepository memberRepository;

    public static String TYPE = "text/csv";

    @Autowired
    private MongoCacheService mongoCacheService;

    private static final Logger log = LoggerFactory.getLogger(DBSourceService.class);

    public DBSourceDto saveDBSource(DBSourceDto dto) throws ParseException, SQLException {
        if (dto.getType() != null && dto.getType().equalsIgnoreCase("sql")) {
            return sqlDBSourceService.saveDBSource(dto);
        } else {
            if (dto.provider.equals(Provider.MONGO)) {
                return mongoService.saveDBSource(dto);
            } else if (dto.provider.equals(Provider.COUCHDB)) {
                return couchService.saveDBSource(dto);
            } else if (dto.provider.equals(Provider.ELASTICSEARCH)) {
                return elasticService.saveDBSource(dto);
            } else {
                return null;
            }
        }
    }

    public boolean checkTempConnection(DBSourceDto dto) throws SQLException {
        if (dto.getType().equalsIgnoreCase("sql")) {
            return sqlDBSourceService.checkTempConnection(dto);
        } else {
            if (dto.provider.equals(Provider.MONGO)) {
                return mongoService.checkTempConnection(dto);
            } else if (dto.provider.equals(Provider.COUCHDB)) {
                return couchService.checkTempConnection(dto);
            } else if (dto.provider.equals(Provider.ELASTICSEARCH)) {
                return elasticService.checkTempConnection(dto);
            } else {
                return false;
            }
        }
    }

    public List<DBSourceDto> getAll() {
        String currentUserEmail = userService.getConnectedUserEmail();
        List<DBSource> allDbsources = repository.findAll();
        List<DBSource> dbSources = new ArrayList<>();
        List<DBSource> personnalDBsources = repository.findAllByUserEmail(currentUserEmail);

        if (memberRepository.findByEmail(currentUserEmail) != null) {
            List<String> memberTeams = memberRepository.findByEmail(currentUserEmail).getTeamIds();
            for (int i = 0; i < memberTeams.size(); i++) {
                String teamId = memberTeams.get(i);
                for (int j = 0; j < allDbsources.size(); j++) {
                    DBSource dbSource = allDbsources.get(j);
                    if (dbSource.getTeamIds().contains(teamId)) {
                        dbSources.add(dbSource);
                    }
                }
            }

            personnalDBsources.forEach(db -> {
                if (!containsName(dbSources, db.getName())) {
                    dbSources.add(db);
                }
            });
            for (int i = 0; i < dbSources.size(); i++) {
                DBSource dbSource = dbSources.get(i);
                dbSource.setOrganizationId(memberRepository.findByEmail(currentUserEmail).getOrganisationId());
                repository.save(dbSource);
            }

            return dbSources.stream().map(db -> {
                encryption.decrypt(db);
                if (db.getType() != null && db.getType().equalsIgnoreCase("sql")) {
                    return this.sqlMapper.mapToDto(db);
                } else {
                    if (db.provider.equals(Provider.MONGO)) {
                        return mongoMapper.mapToDto(db);
                    } else if (db.provider.equals(Provider.COUCHDB)) {
                        return couchMapper.mapToDto(db);
                    } else if (db.provider.equals(Provider.ELASTICSEARCH)) { // Elastic
                        return elasticMapper.mapToDto(db);
                    } else {
                        return null;
                    }
                }
            }).collect(Collectors.toList());
        } else {
            for (int i = 0; i < personnalDBsources.size(); i++) {
                DBSource dbSource = personnalDBsources.get(i);
                dbSource.setOrganizationId(null);
                repository.save(dbSource);
            }
            return personnalDBsources.stream().map(db -> {
                encryption.decrypt(db);
                if (db.getType() != null && db.getType().equalsIgnoreCase("sql")) {
                    return sqlDBSourceService.getTables(this.sqlMapper.mapToDto(db), "all");
                } else {
                    if (db.provider.equals(Provider.MONGO)) {
                        return mongoMapper.mapToDto(db);
                    } else if (db.provider.equals(Provider.COUCHDB)) {
                        return couchMapper.mapToDto(db);
                    } else if (db.provider.equals(Provider.ELASTICSEARCH)) { // Elastic
                        return elasticMapper.mapToDto(db);
                    } else {
                        return null;
                    }
                }
            }).collect(Collectors.toList());
        }
    }

    public void deleteById(String dbsourceId) {
        this.repository.findById(dbsourceId).ifPresent(db -> {
            if (db.getProvider().equals(Provider.MONGO)) {
                this.mongoService.deleteByID(dbsourceId);
            } else if (db.getProvider().equals(Provider.COUCHDB)) {
                this.couchService.deleteByID(dbsourceId);
            } else {
                this.repository.deleteById(dbsourceId);
            }
        });

    }

    public DBSourceDto getDbSourceDtoById(String dbsourceId) {
        return this.repository.findById(dbsourceId).map(db -> {
            encryption.decrypt(db);
            if (db.getType() != null && db.getType().equalsIgnoreCase("sql")) {
                return sqlDBSourceService.getTables(this.sqlMapper.mapToDto(db), "constraints");
            } else {
                if (db.getProvider().equals(Provider.MONGO)) {
                    return this.mongoMapper.mapToDto(db);
                } else if (db.getProvider().equals(Provider.ELASTICSEARCH)) {
                    return this.elasticMapper.mapToDto(db);
                } else if (db.getProvider().equals(Provider.COUCHDB)) {
                    return this.couchMapper.mapToDto(db);
                } else {
                    return null;
                }
            }
        }).orElseThrow();
    }

    public DBSource getDBSourceEntity(String id) {
        return this.repository.findById(id).orElseThrow();
    }

    public boolean addNewCollection(String containerId, String name) {
        try {
            this.containerRepository.findById(containerId).ifPresent(cont -> {
                DBSource dbsource = getDBSourceEntity(cont.getDbsourceId());
                encryption.decrypt(dbsource);

                if (dbsource.getProvider().equals(Provider.MONGO)) {
                    this.mongoService.addNewCollection(dbsource, name);
                } else if (dbsource.getProvider().equals(Provider.ELASTICSEARCH)) {
                    this.elasticService.addNewIndex(dbsource, name);
                }

            });
            return true;
        } catch (Exception e) {
            log.error("add collection error {}", e);
            return false;
        }
    }

    public void dropCollection(String dbsourceId, String databaseName, String name) {
        try {
            this.repository.findById(dbsourceId).ifPresent(db -> {
                encryption.decrypt(db);

                if (db.getProvider().equals(Provider.MONGO)) {
                    this.mongoService.dropCollection(dbsourceId, databaseName, name);
                } else if (db.getProvider().equals(Provider.ELASTICSEARCH)) {
                    this.elasticService.dropIndex(dbsourceId, name);
                }
            });

        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot drop index");
        }
    }

    public boolean updateCollection(String dbsourceId, String databaseName, String oldCollectionName, String newCollectionName) {
        try {
            this.repository.findById(dbsourceId).ifPresent(db -> {
                encryption.decrypt(db);

                if (db.getProvider().equals(Provider.MONGO)) {
                    this.mongoService.renameCollection(dbsourceId, databaseName, oldCollectionName,newCollectionName);
                } else if (db.getProvider().equals(Provider.ELASTICSEARCH)) {
                    //à mettre à jour pour elastic
                    //this.elasticService.dropIndex(dbsourceId, name);

                }
            });
                return true;
        } catch (Exception e) {
            log.error("update collection error {}", e);
            return false;
        }
    }

    public Boolean existsByNameAndUserEmail(String name, String userEmail) {
        return dbSourceRepository.existsByNameAndUserEmail(name, userEmail);
    }

    public String deleteByNameAndUserEmail(String name, String userEmail) {
        dbSourceRepository.deleteByNameAndUserEmail(name, userEmail);
        return name;
    }

    private boolean containsName(final List<DBSource> list, final String name) {
        return list.stream().map(DBSource::getName).anyMatch(db -> db.equalsIgnoreCase(name));
    }

    public String saveCSVtodatabase(MultipartFile file, String dbSourceId, String collection, String database,
                                    Boolean replaceData, String csvFormat) throws IOException {
        String response = "";
        if (TYPE.equals(file.getContentType())) {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));

            char delimiter = ',';
            if (!csvFormat.equals("comma")) {
                delimiter = ';';
            }

            try (CSVParser csvParser = new CSVParser(fileReader,
                    CSVFormat.DEFAULT.withDelimiter(delimiter).withIgnoreEmptyLines().withHeader())) {

                this.dbSourceRepository.findById(dbSourceId).ifPresent(dbSource -> {
                    MongoClient mClient = mongoCacheService.getMongoClient(dbSource);
                    if (mClient != null) {
                        StringBuilder databaseName = new StringBuilder();
                        if (dbSource.getConnectionMode().equalsIgnoreCase("FREE")) {
                            databaseName.append(dbSource.getPhysicalDatabase());
                        } else {
                            databaseName.append(database);
                        }
                        MongoDatabase phdatabase = mClient.getDatabase(databaseName.toString());

                        if (replaceData) {
                            phdatabase.getCollection(collection).drop();
                            phdatabase.createCollection(collection);
                        }

                        List<WriteModel<Document>> writes = writeModelList(csvParser);
                        List<List<WriteModel<Document>>> list = paginateDocuments(writes);

                        BulkWriteOptions bWriteOptions = new BulkWriteOptions();
                        bWriteOptions.ordered(false);
                        list.parallelStream().forEach(g -> {
                            phdatabase.getCollection(collection).bulkWrite(g);
                        });
                    }
                });
            }
            response = "File added successfully";
        } else {
            response = "Make sure that your file is a CSV file";
        }
        return response;
    }

    private List<List<WriteModel<Document>>> paginateDocuments(List<WriteModel<Document>> writes) {
        List<List<WriteModel<Document>>> list = new ArrayList<>();
        int pageSize = writes.size();
        if (writes.size() <= 100) {
            pageSize = 100;
        }
        final int numPages = (int) Math.ceil((double) writes.size() / (double) pageSize);
        for (int pageNum = 0; pageNum < numPages; ) {
            list.add(writes.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, writes.size())));
        }
        return list;
    }

    private List<WriteModel<Document>> writeModelList(CSVParser csvParser) {
        List<Document> documents = new ArrayList<>();
        csvParser.forEach(rec -> {
            Map<String, String> map = new HashMap<>(rec.toMap());
            csvParser.getHeaderMap().keySet().parallelStream().forEach(key -> {
                if (rec.get(key).matches("[0-9]+")) {
                    map.replace(key, rec.get(key), Integer.toString(Integer.parseInt(rec.get(key))));
                }
                if (rec.get(key).matches("([0-9]*)\\.([0-9]*)")) {
                    map.replace(key, rec.get(key), Double.toString(Double.parseDouble(rec.get(key))));
                }
            });
            Document document = new Document();
            document.putAll(map);
            documents.add(document);
        });
        return documents.parallelStream()
                .map(InsertOneModel::new)
                .collect(Collectors.toList());
    }

}
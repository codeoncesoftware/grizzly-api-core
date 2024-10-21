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
package fr.codeonce.grizzly.core.rest;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.DBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.IndexCollection;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceStatsService;
import fr.codeonce.grizzly.core.service.datasource.query.CustomQueryDto;
import fr.codeonce.grizzly.core.service.datasource.sql.SqlDBSourceService;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/dbsource")
public class DataSourceController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    private DBSourceService dbsourceService;

    @Autowired
    private DBSourceRepository dbsourceRepository;

    @Autowired
    private MongoDBSourceService mongoService;

    @Autowired
    private SqlDBSourceService sqlService;

    @Autowired
    private MongoDBSourceStatsService dbSourceStatsService;

    @PostMapping("/create")
    public DBSourceDto saveDatasource(@RequestBody DBSourceDto dto) throws ParseException, SQLException {
        log.info("request to create datasource with name : {}", dto.getName());
        return dbsourceService.saveDBSource(dto);
    }

    @PostMapping("/query/{dbSourceId}")
    public void queryDatabase(@RequestBody CustomQueryDto customQueryDto, @PathVariable String dbSourceId) throws Exception {
        sqlService.executeQuery(dbSourceId, customQueryDto);
    }

    @PostMapping("/check")
    public boolean checkConnection(@RequestBody DBSourceDto dbSourceDto) throws SQLException {
        log.info("request to check validity of the datasource named : {}", dbSourceDto.getName());
        return dbsourceService.checkTempConnection(dbSourceDto);
    }

    @GetMapping("/check/name/{dbsourceName}/{dbsourceId}")
    public boolean checkUnicity(@PathVariable String dbsourceName, @PathVariable String dbsourceId) {
        return mongoService.checkUnicity(dbsourceName, dbsourceId);
    }

    @GetMapping("/all")
    public List<DBSourceDto> getAll() {
        log.info("request to get all datasources");
        return dbsourceService.getAll();
    }

    @DeleteMapping("/delete/{dbsourceId}")
    public void deleteDBSource(@PathVariable String dbsourceId) {
        log.info("request to delete datasource with ID : {}", dbsourceId);
        this.dbsourceService.deleteById(dbsourceId);
    }

    @GetMapping("/{dbsourceId}")
    public DBSourceDto getDbSourceById(@PathVariable String dbsourceId) {
        log.info("request to get datasource DTO with ID : {}", dbsourceId);
        return this.dbsourceService.getDbSourceDtoById(dbsourceId);
    }

    @GetMapping("/stats/{dbsourceId}/{databaseName}/{collectionName}")
    public Document getCollectionStats(@PathVariable String dbsourceId,
                                       @PathVariable String databaseName, @PathVariable String collectionName) {
        log.info(
                "request to get collection stats for datasource with ID : {}, databaseName : {} and collectionName: {}",
                dbsourceId, databaseName, collectionName);
        return this.dbSourceStatsService.getCollectionStats(dbsourceId, databaseName, collectionName);
    }

    @GetMapping("/collection/{dbsourceId}/{databaseName}/{collectionName}")
    public Set<String> getCollectionAttributes(@PathVariable String dbsourceId,
                                               @PathVariable String databaseName, @PathVariable String collectionName) {
        log.info(
                "request to get collection stats for datasource with ID : {}, databaseName : {} and collectionName: {}",
                dbsourceId, databaseName, collectionName);
        DBSource dbSource = dbsourceRepository.findById(dbsourceId).get();
        return this.dbSourceStatsService.getCollectionAttributes(dbSource, databaseName, collectionName);
    }

    @DeleteMapping("/drop/{dbsourceId}/{databaseName}/{collectionName}")
    public void dropCollection(@PathVariable String dbsourceId,
                               @PathVariable String databaseName, @PathVariable String collectionName) {
        log.info("request to drop collection named : {} for datasource with ID : {}", collectionName, dbsourceId);
        this.dbsourceService.dropCollection(dbsourceId, databaseName, collectionName);
    }

    @PostMapping("/addcollection/{containerId}/{collectionName}")
    public boolean addNewCollection(@PathVariable String containerId, @PathVariable String collectionName) {
        log.info("request to create new collection named : {} for container with ID : {}", collectionName, containerId);
        return this.dbsourceService.addNewCollection(containerId, collectionName);
    }

    @PutMapping("/updatecollection/{dbsourceId}/{databaseName}/{oldCollectionName}/{newCollectionName}")
    public boolean updateCollection(@PathVariable String dbsourceId,
                                    @PathVariable String databaseName, @PathVariable String oldCollectionName,@PathVariable String newCollectionName) {
        log.info("request to update collection named : {} for dbsource with ID : {}", oldCollectionName, dbsourceId);
        return this.dbsourceService.updateCollection(dbsourceId, databaseName,oldCollectionName,newCollectionName);
    }
    @GetMapping("/collectionFields/{dbsourceId}/{databaseName}/{collectionName}")
    public Set<String> getFields(@PathVariable String dbsourceId,@PathVariable String databaseName,@PathVariable String collectionName) {
        return this.mongoService.getCollectionFields(dbsourceId, databaseName,collectionName);
    }


    @PostMapping("/createCollectionIndex/{dbsourceId}/{databaseName}/{collectionName}")
    public void createCollectionIndex(@PathVariable String dbsourceId,@PathVariable String databaseName,@PathVariable String collectionName,   @RequestBody IndexCollection indexRequest) {
        this.mongoService.createCollectionIndex(dbsourceId, databaseName, collectionName,indexRequest);
    }

    @DeleteMapping("/deleteCollectionIndex/{dbsourceId}/{databaseName}/{collectionName}/{indexName}")
    public void deleteCollectionIndex(@PathVariable String dbsourceId,@PathVariable String databaseName,@PathVariable String collectionName,  @PathVariable String indexName) {
        log.info("request to delete Collection Index with name : {}", indexName);
        this.mongoService.deleteCollectionByIndex(dbsourceId, databaseName, collectionName,indexName);
    }

    @GetMapping("/getCollectionIndexes/{dbsourceId}/{databaseName}/{collectionName}")
    public List<IndexCollection> getCollectionIndexes(@PathVariable String dbsourceId, @PathVariable String databaseName, @PathVariable String collectionName) {
       return  this.mongoService.getCollectionIndexes(dbsourceId, databaseName, collectionName);
    }

    @GetMapping("/public")
    public DBSourceDto getDBSource(@RequestParam String dbsourceId) {
        log.info("request to get DBSource with ID : {}", dbsourceId);
        return this.dbsourceService.getDbSourceDtoById(dbsourceId);
    }

    @GetMapping("/public/getdbsource")
    public DBSource getDBSourceEntity(@RequestParam String dbsourceId) {
        return this.dbsourceService.getDBSourceEntity(dbsourceId);
    }

    @GetMapping("/existsByName/{name}/{userEmail}")
    public Boolean existsByName(@PathVariable String name, @PathVariable String userEmail) {
        return dbsourceService.existsByNameAndUserEmail(name, userEmail);
    }

    @DeleteMapping("/deleteByNameAndEmail/{name}/{userEmail}")
    public String deleteByName(@PathVariable String name, @PathVariable String userEmail) {
        return dbsourceService.deleteByNameAndUserEmail(name, userEmail);
    }

    @PostMapping("/saveCSVtodatabase/{dbSourceId}/{collection}/{database}/{replaceData}/{csvFormat}")
    public String saveCSVtodatabase(@RequestParam MultipartFile file, @PathVariable String dbSourceId,
                                    @PathVariable String collection, @PathVariable String database, @PathVariable Boolean replaceData, @PathVariable String csvFormat) throws IOException {
        log.info("request to upload csv file into database : {}", file.getOriginalFilename());
        return dbsourceService.saveCSVtodatabase(file, dbSourceId, collection, database, replaceData, csvFormat);

    }
}

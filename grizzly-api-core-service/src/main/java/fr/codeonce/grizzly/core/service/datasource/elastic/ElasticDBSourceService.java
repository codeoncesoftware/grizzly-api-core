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
package fr.codeonce.grizzly.core.service.datasource.elastic;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.IDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.elastic.mapper.ElasticDBSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ElasticDBSourceService implements IDBSourceService {

    private static final String DEFAULT_ADMIN_JSON = "{ \"firstname\": \"Administrator\", \"lastname\": \"Grizzly\", \"username\": \"admin\", \"password\": \"admin\", \"roles\": [\"admin\"]}";

    @Autowired
    private ElasticCacheService cacheService;

    @Autowired
    private ElasticDBSourceMapperService mapper;

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    @Qualifier("coreRestTemplate")
    private RestTemplate restTemplate;

    @Value("${frontUrl}")
    private String url;

    private static final Logger log = LoggerFactory.getLogger(ElasticDBSourceService.class);

    @Autowired
    private CryptoHelper encryption;

    public DBSourceDto saveDBSource(DBSourceDto dto) throws ParseException {
        DBSource elasticSource = mapper.mapToDomain(dto);
        encryption.encrypt(elasticSource);
        elasticSource = repository.save(elasticSource);
        encryption.decrypt(elasticSource);
        updateCacheInstance(elasticSource);
        if (!StringUtils.isAllBlank(dto.getId()) && dto.isAuthDBEnabled()) {
            addDefaultIndex(elasticSource);
        }
        return mapper.mapToDto(elasticSource);
    }

    public boolean checkTempConnection(DBSourceDto dto) {
        try {
            return this.cacheService.buildRestHighLevelClient(mapper.mapToDomain(dto)).ping(RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.warn("Can't get status : {}", e.getMessage());
            return false;
        }
    }

    public Boolean checkStatus(DBSourceDto dto) {
        try {
            return this.cacheService.getClient(mapper.mapToDomain(dto)).ping(RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.warn("Can't get status : {}", e.getMessage());
            return false;
        }
    }

    public List<String> getIndicesList(DBSource db) {
        GetIndexRequest request = new GetIndexRequest().indices("*");
        GetIndexResponse response = null;
        try {
            response = this.cacheService.getClient(db).indices().get(request, RequestOptions.DEFAULT);
            return Arrays.asList(response.getIndices());
        } catch (Exception e) {
            log.warn("Cannot retrieve list of indices : {}", e.getMessage());
            return new ArrayList<>();
        }

    }

    public void addNewIndex(DBSource dbsource, String name) {
        RestClient client = this.cacheService.getRestClient(dbsource.getId());
        Request request = new Request("HEAD", "/" + name);
        try {
            Response x = client.performRequest(request);
            if (x.getStatusLine().toString().contains("404")) {
                Request addIndex = new Request("PUT", "/" + name);
                try {
                    client.performRequest(addIndex);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot connect to elasticsearch provider");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot connect to elasticsearch provider");
        }

    }

    public void dropIndex(String dbsourceId, String name) {
        RestClient client = this.cacheService.getRestClient(dbsourceId);
        Request request = new Request("DELETE", "/" + name);
        try {
            client.performRequest(request);
        } catch (IOException e) {
            log.warn("Can't drop Index : {}", e.getMessage());
            throw new IllegalArgumentException("Cannot delete Index");
        }

    }

    /**
     * Update Cache Instances for Core And Send HTTP Request for Runtime to Update it's cache
     *
     * @param db
     */
    private void updateCacheInstance(DBSource db) {
        this.cacheService.updateCache(db);
        // this.updateRuntimeCache(restTemplate, url, db.getId(), Provider.ELASTICSEARCH);
    }

    /**
     * Add Default Auth_user Collection for Roles Handling
     *
     * @param dbsource
     * @throws ParseException
     */
    public void addDefaultIndex(DBSource dbsource) throws ParseException {
        String indexName = "authentication_user";

        addNewIndex(dbsource, indexName);
        UUID uuid = UUID.randomUUID();
        RestClient client = this.cacheService.getRestClient(dbsource.getId());
        Request search = new Request("GET", "/" + indexName + "/_search");
        try {
            // insertion de l'admin une seule fois
            InputStream inputStream = client.performRequest(search).getEntity().getContent();

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(
                    new InputStreamReader(inputStream, "UTF-8"));
            JSONObject hits = (JSONObject) jsonObject.get("hits");
            JSONObject total = (JSONObject) hits.get("total");
            Long value = (Long) total.get("value");
            if (value == 0) {
                String body = DEFAULT_ADMIN_JSON;
                try (NStringEntity entity = new NStringEntity(body, ContentType.APPLICATION_JSON)) {
                    Request request = new Request("PUT", "/" + indexName + "/doc/" + uuid.toString());
                    request.setEntity(entity);
                    try {
                        client.performRequest(request);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Cannot connect to elasticsearch provider");
                    }
                }

            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot connect to elasticsearch provider");
        }

    }

}

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
package fr.codeonce.grizzly.core.service.datasource.couchdb;

import com.couchbase.client.java.Bucket;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.IDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.couchdb.mapper.CouchDBSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CouchDBSourceService implements IDBSourceService {

    @Autowired
    private CouchCacheService cacheService;

    @Autowired
    private CouchDBSourceMapperService mapper;

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    @Qualifier("coreCryptoHelper")
    private CryptoHelper encryption;

    @Autowired
    @Qualifier("coreRestTemplate")
    private RestTemplate restTemplate;

    @Value("${frontUrl}")
    private String url;

    private static final Logger log = LoggerFactory.getLogger(CouchDBSourceService.class);

    public DBSourceDto saveDBSource(DBSourceDto dto) {
        DBSource couchSource = mapper.mapToDomain(dto);
        encryption.decrypt(couchSource);
        updateCacheInstance(couchSource);
        encryption.encrypt(couchSource);
        return mapper.mapToDto(repository.save(couchSource));
    }

    private void updateCacheInstance(DBSource couchSource) {

        if (!StringUtils.isAllBlank(couchSource.getId())) {
            this.cacheService.updateCache(couchSource);
            this.updateRuntimeCache(restTemplate, url, couchSource.getId(), Provider.COUCHDB);
        }

    }

    public boolean checkConnection(DBSourceDto dto) {
        try {
            String res = this.cacheService.getCluster(dto.getHost()).diagnostics().exportToJson();
            return !StringUtils.isEmpty(res);
        } catch (Exception e) {
            log.warn("Error Pinging the Ckuster : {}", e.getCause());
            return false;
        }
    }

    public void deleteByID(String id) {
        this.repository.deleteById(id);

    }

    public boolean checkTempConnection(DBSourceDto dto) {
        try {
            Bucket bucket = this.cacheService.connectToTempBucket(dto);
            return String.valueOf(bucket.ping().endpoints().get(0).get(0).state()).equals("OK");
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getBucketsList(String node) {
        try {
            List<String> buckets = new ArrayList<>();
            this.cacheService.getCluster(node).buckets().getAllBuckets().forEach((a, b) -> {
                buckets.add(a);
            });
            return buckets;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}

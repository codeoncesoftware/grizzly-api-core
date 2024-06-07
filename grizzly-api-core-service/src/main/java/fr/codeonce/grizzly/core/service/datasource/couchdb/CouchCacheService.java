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
import com.couchbase.client.java.Cluster;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CouchCacheService {

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    @Qualifier("coreCryptoHelper")
    private CryptoHelper encryption;

    @Cacheable(value = "buckets", key = "#dto.id")
    public Bucket connectToBucket(DBSourceDto dto) {
        Cluster cluster = getCluster(dto.getHost());
        return getBucket(cluster, dto.getBucketName(), String.valueOf(dto.getPassword()));
    }

    @Cacheable(value = "buckets", key = "#entity.id")
    public Bucket connectToBucket(DBSource entity) {
        return buildBucket(entity);
    }

    @Cacheable(value = "buckets", key = "#sourceId")
    public Bucket getBucket(String sourceId) {
        return this.repository.findById(sourceId).map(db -> {
            encryption.decrypt(db);
            return connectToBucket(db);
        }).orElseThrow();

    }

    public Cluster getCluster(String node) {
        try {
            return Cluster.connect(node, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Bucket informations are invalid");
        }
    }

    private Bucket buildBucket(DBSource entity) {
        Cluster cluster = getCluster(entity.getHost());
        return getBucket(cluster, entity.getBucketName(), String.valueOf(entity.getPassword()));
    }

    private Bucket getBucket(Cluster cluster, String name, String password) {
        if (!StringUtils.isEmpty(password)) {
            return cluster.bucket(name);
        } else {
            return cluster.bucket(name);
        }

    }

    public Bucket connectToTempBucket(DBSourceDto dto) {
        Cluster cluster = Cluster.connect(dto.getHost(), dto.getUsername(), String.valueOf(dto.getPassword()));
        return getBucket(cluster, dto.getBucketName(), ObjectUtils.defaultIfNull(dto.getPassword(), StringUtils.EMPTY).toString());
    }

    @CachePut(value = "buckets", key = "#couchSource.id")
    public Bucket updateCache(DBSource couchSource) {
        return buildBucket(couchSource);
    }
}

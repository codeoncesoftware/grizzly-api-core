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
package fr.codeonce.grizzly.core.service.datasource.mongo.mapper;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;


@Service
public class MongoDBSourceMapperService extends MongoDBSourceMapperImpl {

    private static final Logger log = LoggerFactory.getLogger(MongoDBSourceMapperService.class);

    @Lazy
    @Autowired
    private MongoDBSourceService dbService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public DBSourceDto mapToDto(DBSource entity) {

        StopWatch watch = new StopWatch();
        watch.start("Fetching Databases");

        DBSourceDto dto = null;
        try {
            dto = super.mapToDto(entity);
            dto.setDatabases(dbService.getDBdatabasesList(dto));
            dto.setActive(dbService.checkConnection(dto));
        } catch (IllegalStateException e) {
            // MongoClient Expired
            cacheManager.getCache("mongoClients").clear();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
        watch.stop();
        log.debug(watch.prettyPrint());

        return dto;
    }
}

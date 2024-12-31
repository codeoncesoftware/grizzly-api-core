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
package fr.codeonce.grizzly.core.service.datasource.elastic.mapper;

import fr.codeonce.grizzly.core.domain.datasource.CustomDatabase;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.elastic.ElasticDBSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ElasticDBSourceMapperService extends ElasticDBSourceMapperImpl {

    @Autowired
    private ElasticDBSourceService elasticService;

    @Override
    public DBSourceDto mapToDto(DBSource entity) {
        DBSourceDto dto = super.mapToDto(entity);
        dto.setActive(elasticService.checkStatus(dto));
        CustomDatabase db = new CustomDatabase();
        db.setName(entity.getName());
        db.setCollections(elasticService.getIndicesList(entity));
        dto.setDatabases(Collections.singletonList(db));
        return dto;
    }

}

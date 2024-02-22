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
package fr.codeonce.grizzly.core.service.datasource.marklogic;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.IDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.marklogic.mapper.MarklogicDBSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class MarklogicDBSourceService implements IDBSourceService {

	@Autowired
	private MarklogicCacheService cacheService;

	@Autowired
	private MarklogicDBSourceMapperService mapper;

	@Autowired
	private DBSourceRepository repository;

	@Autowired
	@Qualifier("coreRestTemplate")
	private RestTemplate restTemplate;

	@Autowired
	private CryptoHelper encryption;
	
	@Value("${frontUrl}")
	private String url;

	private static final Logger log = LoggerFactory.getLogger(MarklogicDBSourceService.class);

	public DBSourceDto saveDBSource(DBSourceDto dto) {
		DBSource source = mapper.mapToDomain(dto);
		encryption.encrypt(source);
		source = repository.save(source);
		encryption.decrypt(source);
		updateCacheInstance(source);
		return mapper.mapToDto(source);
	}

	private void updateCacheInstance(DBSource source) {

		this.cacheService.updateCache(source);
		this.updateRuntimeCache(restTemplate, url, source.getId(), Provider.MARKLOGIC);

	}

	public boolean checkTempConnection(DBSourceDto dto) {
		// TODO to implement this method
		return true;
	}

	public boolean checkStatus(DBSourceDto dto) {
		OkHttpClient client = (OkHttpClient) this.cacheService.getClient(dto.getId()).getClientImplementation();
		Response response;
		try {
			response = client.newCall(new Request.Builder()
					.url(new HttpUrl.Builder().scheme("http").host("localhost").port(8002).encodedPath("/").build())
					.build()).execute();
			int statusCode = response.code();
			if (statusCode >= 400) {
				throw new RuntimeException(statusCode + " " + response.message());
			}
			return true;
		} catch (IOException e) {
			log.error("Can't connect to marklogic source {}" , e);
			return false;
		}
	}
}

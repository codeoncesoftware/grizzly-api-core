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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;

@Service
public class MarklogicCacheService {

	@Autowired
	private DBSourceRepository repository;

	@Autowired
	@Qualifier("coreCryptoHelper")
	private CryptoHelper encryption;

	private static final Logger log = LoggerFactory.getLogger(MarklogicCacheService.class);

	/**
	 * Authenticate the given user with MarkLogic database, throw exception if
	 * unsuccessful since user has no permissions to access the database.
	 */
	@Cacheable(value = "marklogicClients", key = "#dbsourceId")
	public DatabaseClient getClient(String dbsourceId) {
		return this.repository.findById(dbsourceId).map(this::getClient).orElseThrow();
	}
	
	public DatabaseClient getClient(DBSource db) {
		encryption.decrypt(db);
		return DatabaseClientFactory.newClient(db.getHost(), db.getPort(),
				new DatabaseClientFactory.DigestAuthContext(db.getUsername(), String.valueOf(db.getPassword())));
	}

	@CachePut(value = "marklogicClients", key = "#source.id")
	public DatabaseClient updateCache(DBSource source) {

		return getClient(source);
		
	}
}

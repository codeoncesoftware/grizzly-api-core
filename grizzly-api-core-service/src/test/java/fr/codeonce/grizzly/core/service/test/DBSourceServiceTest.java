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
package fr.codeonce.grizzly.core.service.test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;

public class DBSourceServiceTest extends AbstractServiceTest {

	@Autowired
	private MongoDBSourceService mongoDBSourceService;

	@MockBean
	private MongoCacheService mongoCacheService;

	private Authentication auth;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		User user = new User();
		user.setEmail("test@codeonce.fr");
		auth = new UsernamePasswordAuthenticationToken(user, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	public void testCheckUnicity() {
		DBSourceDto dbSourceDto = new DBSourceDto();
		DBSource dbSource = new DBSource();
		dbSource.setHost("127.0.0.999");
		dbSourceDto.setHost("127.0.0.999");
		given(dbSourceRepository.findAllByUserEmail(auth.getName())).willReturn(Collections.singletonList(dbSource));
		assertEquals(dbSource.getHost(), mongoDBSourceService.getAll().get(0).getHost());
	}

	@Test(expected = NoSuchElementException.class)
	public void testGetDBSourceByIdException() {
		mongoDBSourceService.getDbSourceById("dbsourceId");
	}

	@Test
	public void testCheckConnection() {
		ServerAddress serverAddress = new ServerAddress("127.0.0.0", 27272);
		MongoClientSettings settings = MongoClientSettings.builder().applyToClusterSettings(sett -> {
			sett.hosts(Collections.singletonList(serverAddress));
			sett.serverSelectionTimeout(2000, TimeUnit.MILLISECONDS);
		}).applyToSocketSettings(sett -> {
			sett.connectTimeout(1000, TimeUnit.MILLISECONDS);
		}).build();
		MongoClient mongoClient = MongoClients.create(settings);
		DBSource dbsource = new DBSource();
		dbsource.setConnectionMode("FREE");
		dbsource.setDatabase("testDB");
		doReturn(mongoClient).when(mongoCacheService).getMongoClient(dbsource);

		assertThatIllegalArgumentException();

	}

}

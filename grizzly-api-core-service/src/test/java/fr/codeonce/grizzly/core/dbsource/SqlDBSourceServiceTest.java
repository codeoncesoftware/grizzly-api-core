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
//package fr.codeonce.grizzly.core.dbsource;
//
//import java.sql.SQLException;
//
//import org.junit.Test;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.MockitoJUnitRunner;
//
//
//import fr.codeonce.grizzly.core.domain.datasource.DBSource;
//import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
//import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
//import fr.codeonce.grizzly.core.service.datasource.sql.SqlDBSourceService;
//import junit.framework.Assert;
//
//@RunWith(MockitoJUnitRunner.class)
//public class SqlDBSourceServiceTest {
//
//	@Mock
//	DBSourceRepository dbSourceRepository;
//	
//	@InjectMocks
//	SqlDBSourceService sqlDBSourceService = new SqlDBSourceService();
//	
//	
//	@BeforeEach
//	public void init() {
//		MockitoAnnotations.initMocks(this);
//	}
//	
//	@Test
//	public void saveSQLDBSource() throws SQLException {
//		DBSourceDto dto = new DBSourceDto();
//		dto.setName("demo");
//		dto.setType("sql");
//		DBSource dbSource = new DBSource();
//		dbSource.setName("demo");
//		dbSource.setType("sql");
//		
//		Mockito.when(dbSourceRepository.save(Mockito.any())).thenReturn(dbSource);
//		
//		DBSourceDto createdDBSource = sqlDBSourceService.saveDBSource(dto);
//		Assert.assertEquals(dto, createdDBSource);
//
//	}
//}

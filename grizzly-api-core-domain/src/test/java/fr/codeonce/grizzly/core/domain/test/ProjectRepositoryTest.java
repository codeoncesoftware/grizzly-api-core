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
//package fr.codeonce.grizzly.core.domain.test;
//
//import java.util.Optional;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import fr.codeonce.grizzly.core.domain.project.Project;
//import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
//
//
//
//public class ProjectRepositoryTest extends AbstractJMongoTest {
//
//	@Autowired
//	private ProjectRepository projectRepository;
//
//	@Before
//	public void init() {
//		Project project = new Project();
//		project.setId("ID-1");
//		project.setName("MY-PROJECT");
//		projectRepository.save(project);
//	}
//
//	@Test
//	public void save() {
//		Project savedProject = projectRepository.save(new Project());
//		Assert.assertNotNull("id should not be null", savedProject.getId());
//		Assert.assertNotNull("creation time should not be null", savedProject.getCreationTime());
//	}
//
//	@Test
//	public void findEmptyByProjectName() {
////		 Optional<Project> optional = projectRepository.findByName("no project");
////		 Assert.assertTrue("project should not be found", optional.isEmpty());
//	}
//
//	@Test
//	public void findOneByProjectName() {
////		Optional<Project> optional = projectRepository.findByProjectName("MY-PROJECT");
////		Assert.assertFalse("project should be found by its name", optional.isEmpty());
//	}
//
//}

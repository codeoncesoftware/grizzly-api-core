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
package fr.codeonce.grizzly.core.service.test.project;

//public class ProjectServiceTest extends AbstractServiceTest {
//
//    private static final String MY_ID = "12345";
//
//    private static final String MY_PROJECT_NAME = "myProject";
//
//    private static final Logger log = LoggerFactory.getLogger(ProjectServiceTest.class);
//
//    @Autowired
//    private ProjectService projectService;
//
//    private List<Project> testProjects = new ArrayList<>();
//    private Project testProject = new Project();
//    private SecurityApiConfig sec = new SecurityApiConfig();
//    private ProjectDto testProjectDto = new ProjectDto();
//    private SecurityApiConfigDto secDto = new SecurityApiConfigDto();
//    private DBSource testDBSource = new DBSource();
//    private DBSourceDto testDBSourceDto = new DBSourceDto();
//
//    @Before
//    public void init() {
//        testProjects = new ArrayList<Project>();
//
//        testProject = new Project();
//        testDBSource = new DBSource();
//        testDBSource.setId("dbId");
//        testDBSource.setConnectionMode("FREE");
//        sec = new SecurityApiConfig();
//        testProject.setId(MY_ID);
//        testProject.setName(MY_PROJECT_NAME);
//        testProject.setDescription("mydesc");
//        testProject.setDbsourceId("dbId");
//        testProject.setDatabaseName("db");
//        sec.setClientId("clientId");
//        sec.setSecretKey("secretKey");
//        sec.setTokenExpiration(3600);
//        testProject.setSecurityConfig(sec);
//        testProjects.add(testProject);
//
//        testProjectDto = new ProjectDto();
//        testDBSourceDto = new DBSourceDto();
//        testDBSourceDto.setId("dbId");
//        secDto = new SecurityApiConfigDto();
//        testProjectDto.setId(MY_ID);
//        testProjectDto.setName(MY_PROJECT_NAME);
//        testProjectDto.setDescription("mydesc");
//        testProjectDto.setDescription("mydesc");
//        testProjectDto.setDbsourceId("dbId");
//        secDto.setClientId("clientId");
//        secDto.setSecretKey("secretKey");
//        secDto.setTokenExpiration(3600);
//        testProjectDto.setSecurityConfig(secDto);
//        SecurityContextUtil.setUpSecurityContext("team@codeonce.fr");
//
//    }
//
//    @Test
//    public void testGetAllProjects() {
//
//        // Call
//        given(projectRepository.findAll()).willReturn(testProjects);
//
//        // Test
//        List<ProjectDto> allProjects = projectService.getAll();
//        assertFalse(allProjects.isEmpty());
//        assertEquals(MY_PROJECT_NAME, testProject.getName());
//    }
//
//    @Test
//    public void testGetproject() {
//        // Call
//        given(projectRepository.findById(MY_ID)).willReturn(Optional.of(testProject));
//
//        // Test
//
//        ProjectDto myProjectDto = projectService.get(MY_ID);
//        Assert.assertFalse(myProjectDto == null);
//        assertEquals(MY_PROJECT_NAME, myProjectDto.getName());
//    }
//
//    @Test(expected = NoSuchElementException.class)
//    public void testGetNotFoundProject() {
//
//        // Mock
//        given(projectRepository.findById(MY_ID)).willReturn(Optional.empty());
//
//        // Test
//        projectService.get(MY_ID);
//
//    }
//
////	@Test
////	public void testCreateProject() {
////
////		// Call
////		given(dbSourceRepository.findById("dbId")).willReturn(Optional.of(testDBSource));
////		given(projectRepository.save(any())).willReturn(testProject);
////
////		// Test
////		ProjectDto myCreatedProjectDto = projectService.createProject(testProjectDto);
////		assertFalse(myCreatedProjectDto == null);
////		assertEquals(MY_PROJECT_NAME, myCreatedProjectDto.getName());
////
////	}
//
////	@Test
////	public void testUpdateProject() {
////		// Data
////
////		ProjectDto myNewProjectDto = new ProjectDto();
////		myNewProjectDto.setId("12345");
////		myNewProjectDto.setName("hello_project");
////		myNewProjectDto.setDescription("mydesc1");
////		log.info("rmlkork : " + myNewProjectDto.getName());
////
////		// Call
////		given(projectRepository.findById(MY_ID)).willReturn(Optional.of(testProject));
////
////		// Test
////		ProjectDto myProject = projectService.update(myNewProjectDto, MY_ID);
////		log.info("" + myProject);
////		Assert.assertTrue(myProject.getName().equals(myNewProjectDto.getName()));
////		Assert.assertTrue(myProject.getDescription().equals(myNewProjectDto.getDescription()));
////
////	}
////
////	@Test(expected = NoSuchElementException.class)
////	public void testUpdateNonExsistingProject() {
////		// Data
////
////		ProjectDto myNewProjectDto = new ProjectDto();
////		myNewProjectDto.setId("12345");
////		myNewProjectDto.setName("hello_project");
////		myNewProjectDto.setDescription("mydesc1");
////		log.info("rmlkork : " + myNewProjectDto.getName());
////
////		// Call
////		given(projectRepository.findById(MY_ID)).willReturn(Optional.empty());
////
////		// Test
////		ProjectDto myProject = projectService.update(myNewProjectDto, MY_ID);
////		log.info("" + myProject);
////		Assert.assertTrue(myProject.getName().equals(myNewProjectDto.getName()));
////		Assert.assertTrue(myProject.getDescription().equals(myNewProjectDto.getDescription()));
////
////	}
//
//    @Test(expected = NoSuchElementException.class)
//    public void testDeleteNotFoundProject() {
//
//        // Mock
//        given(projectRepository.findById(MY_ID)).willReturn(Optional.empty());
//
//        // Test
//        projectService.delete(MY_ID);
//        Mockito.verify(projectRepository, times(1)).delete(testProject);
//
//    }
//
//    @Test
//    public void testDeleteExisitingProject() {
//        // Mock
//        given(projectRepository.findById(MY_ID)).willReturn(Optional.of(testProject));
//
//        // Call
//        projectService.delete(MY_ID);
//        Mockito.verify(projectRepository, times(1)).delete(testProject);
//    }
//
//    @Test
//    public void testDeleteAllProjects() {
//        // Call
//        projectService.deleteAll();
//        // Verify
//        Mockito.verify(projectRepository, times(1)).deleteAll();
//    }
//
//    @Test
//    public void testIsProjectNameTaken() {
//        // Mock
//        given(projectRepository.findByNameIgnoreCaseAndUserEmail(MY_PROJECT_NAME, "team@codeonce.fr"))
//                .willReturn(Optional.of(testProject));
//
//        // Call
//        boolean projectNameTaken = projectService.existsProjectName(MY_PROJECT_NAME, "other_project_id");
//        assertTrue(projectNameTaken);
//
//    }
//
//}

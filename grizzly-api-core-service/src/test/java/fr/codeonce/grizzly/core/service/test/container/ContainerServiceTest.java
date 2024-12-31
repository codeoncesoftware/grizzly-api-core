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
package fr.codeonce.grizzly.core.service.test.container;

//@TestPropertySource(properties = "spring.mongodb.embedded.version=3.5.5")
//public class ContainerServiceTest extends AbstractServiceTest {
//
//    @Autowired
//    private ContainerService containerService;
//
//    @MockBean
//    private MongoCacheService mongoCacheService;
//
//    @Autowired
//    private KeycloakOauthService keycloakOauthService;
//
//    private static final String MY_ID = "12345";
//
//    private static final String MY_CONTAINER_NAME = "myContainer";
//
//
//    private List<Container> testContainers = new ArrayList<>();
//    private Container testContainer = new Container();
//    private ContainerDto testContainerDto = new ContainerDto();
//    private Project testProject = new Project();
//    private ProjectDto testProjectDto = new ProjectDto();
//    private ContainerHierarchy testContainerhierarchy = new ContainerHierarchy();
//    private List<Container> containersTest = new ArrayList<>();
//    private List<Container> containersTestNoSecurity = new ArrayList<>();
//    Container containerTest = new Container();
//    ProjectDto projectTest = new ProjectDto();
//    Project project = new Project();
//    ProjectDto projectTestNoSecurity = new ProjectDto();
//    Container containerTestNoSecurity = new Container();
//    ProjectDto projectTestAuthMS = new ProjectDto();
//    IdentityProvider identityProvider = new IdentityProvider();
//
//    @Before
//    public void init() {
//
//        testProject = new Project();
//        testProject.setId("123");
//        testProject.setName("heyhey");
//
//        testProjectDto = new ProjectDto();
//        testProjectDto.setId("123");
//        testProjectDto.setName("heyhey");
//
//        testContainers = new ArrayList<Container>();
//        testContainer = new Container();
//        testContainer.setId(MY_ID);
//        testContainer.setName(MY_CONTAINER_NAME);
//        testContainer.setProjectId(testProject.getId());
//        testContainer.setHierarchyId(MY_ID);
//        testContainers.add(testContainer);
//
//        testContainerDto = new ContainerDto();
//        testContainerDto.setId(MY_ID);
//        testContainerDto.setName(MY_CONTAINER_NAME);
//        testContainerDto.setProjectId(testProjectDto.getId());
//
//        List<Resource> resources = new ArrayList<>();
//        List<ResourceGroup> resourcesGroup = new ArrayList<>();
//        Resource rc = new Resource();
//        CustomQuery cQ = new CustomQuery("test", "test", "test", "test", "test");
//        rc.setResourceGroup("Authentication Grizzly");
//        rc.setCustomQuery(cQ);
//        resources.add(rc);
//        ResourceGroup rg = new ResourceGroup();
//        rg.setName("Authentication Grizzly");
//        resourcesGroup.add(rg);
//        containerTest.setProjectId("test");
//        containerTest.setResources(resources);
//        containerTest.setResourceGroups(resourcesGroup);
//        containersTest.add(containerTest);
//
//        projectTest.setId("test");
//        projectTest.setSecurityEnabled(true);
//        projectTest.setType("microservice");
//        projectTest.setDbsourceId("testDbSourceId");
//
//        projectTestNoSecurity.setId("testNoSecurity");
//        projectTestNoSecurity.setSecurityEnabled(false);
//        projectTestNoSecurity.setType("microservice");
//
//        containerTestNoSecurity.setProjectId("testNoSecurity");
//        containersTestNoSecurity.add(containerTestNoSecurity);
//
//        projectTestAuthMS.setId("testAuthMS");
//        projectTestAuthMS.setSecurityEnabled(true);
//        projectTestAuthMS.setType("authentication microservice");
//        projectTest.setDbsourceId("testDbSourceIdAuthMS");
//
//        project.setId("test");
//        project.setSecurityEnabled(true);
//        project.setType("microservice");
//        project.setDbsourceId("testDbSourceId");
//        List<String> idpIds = new ArrayList<>();
//        project.setIdentityProviderIds(idpIds);
//
//        identityProvider.setId("idpID");
//        identityProvider.setName(IdentityProviders.KEYCLOAK);
//    }
//
//    // @Test
//    public void testGetAllContainers() {
//
//        // Call
//        given(containerRepository.findAll()).willReturn(testContainers);
//
//        // Test
//        List<ContainerDto> allContainers = containerService.getAll();
//        assertFalse(allContainers.isEmpty());
//        assertEquals(MY_CONTAINER_NAME, testContainer.getName());
//    }
//
//    // @Test
//    public void testGetContainer() {
//        // Call
//        given(containerRepository.findById(MY_ID)).willReturn(Optional.of(testContainer));
//
//        // Test
//
//        ContainerDto myContainerDto = containerService.get(MY_ID);
//        Assert.assertFalse(myContainerDto == null);
//        assertEquals(MY_CONTAINER_NAME, myContainerDto.getName());
//    }
//
//    @Test(expected = NoSuchElementException.class)
//    public void testGetNotFoundContainer() {
//
//        // Mock
//        given(containerRepository.findById(MY_ID)).willReturn(Optional.empty());
//
//        // Test
//        ContainerDto c = containerService.get(MY_ID);
//        Assert.assertTrue(c == null);
//
//    }
//
//    // @Test
//    public void testCreateContainerWithProject() throws CustomGitAPIException {
//
//        // Call
//        given(projectRepository.findById(testProject.getId())).willReturn(Optional.of(testProject));
//        given(containerRepository.save(any())).willReturn(testContainer);
//
//        // Test
//
//        ContainerDto myCreatedContainerDto = containerService.saveContainer(testContainerDto, false);
//        assertFalse(myCreatedContainerDto == null);
//        assertEquals(MY_CONTAINER_NAME, myCreatedContainerDto.getName());
//
//    }
//
//    @Test(expected = NoSuchElementException.class)
//    public void testCreateContainerWithoutProject() throws CustomGitAPIException {
//
//        // Call
//        given(projectRepository.findById(testProject.getId())).willReturn(Optional.empty());
//
//        // Test
//        ContainerDto myCreatedContainerDto = containerService.saveContainer(testContainerDto, false);
//        assertTrue(myCreatedContainerDto == null);
//
//    }
//
//    @Test
//    public void testDeleteExisitingContainer() {
//        // Mock
//        given(containerRepository.findById(MY_ID)).willReturn(Optional.of(testContainer));
//        given(containerHierarchyRepository.findById(MY_ID)).willReturn(Optional.of(testContainerhierarchy));
//        given(mongoCacheService.getGridFs(any(), any())).willReturn(gridFsTemplate);
//        // Call
//        containerService.delete(MY_ID);
//        Mockito.verify(containerRepository, times(1)).delete(testContainer);
//    }
//
//    @Test(expected = NoSuchElementException.class)
//    public void testDeleteNotFoundContainer() {
//
//        // Mock
//        given(containerRepository.findById(MY_ID)).willReturn(Optional.empty());
//
//        // Test
//        containerService.delete(MY_ID);
//
//    }
//
//    @Test
//    public void testDeleteAllContainers() {
//
//        // Call
//        containerService.deleteAll();
//        // Verify
//        Mockito.verify(containerRepository, times(1)).deleteAll();
//    }
//
//    // @Test
//    public void testExistsContainerName() {
//        // Mock
//        given(containerRepository.findAllByProjectId(testProject.getId())).willReturn(testContainers);
//
//        // Test
//        boolean containerNameTaken = containerService.existsContainerName(testContainerDto);
//
//        assertTrue(containerNameTaken);
//
//    }
//
//    // @Test
//    public void testContainersByProject() {
//        // Mock
//        given(containerRepository.findAllByProjectId(testProject.getId())).willReturn(testContainers);
//
//        // Test
//        List<ContainerDto> listContainers = containerService.containersByProject(testProject.getId());
//        assertFalse(listContainers.isEmpty());
//
//    }
//
//    @Test
//    public void testDeleteAllByProject() {
//        containerService.deleteAllByProject(MY_ID);
//        Mockito.verify(containerRepository, times(1)).deleteContainerByProjectId(MY_ID);
//    }
//
////	@Test
////	public void importContainerTest() throws IOException {
////		File zipFile = ResourceUtils.getFile("classpath:fs/xsl.zip");
////		String projectId = "5ce2955a3f4434b488096325";
////		String containerName = "hello_world";
////		String x = containerService.importContainer(zipFile, containerName, projectId);
////		Assert.assertEquals(projectId,x);
////	}
//
//    @Test
//    public void updateResourcesDbsourceId() {
//        given(containerRepository.findAllByProjectId("test")).willReturn(containersTest);
//        containerService.updateResourcesDbsourceId("test", "testUpdate");
//        assertEquals("testUpdate", containerTest.getResources().get(0).getCustomQuery().getDatasource());
//    }
//
//    @Test
//    public void updateResources() {
//        given(containerRepository.findAllByProjectId("test")).willReturn(containersTest);
//        given(containerRepository.findAllByProjectId("testNoSecurity")).willReturn(containersTestNoSecurity);
//        DBSource dbSource = new DBSource();
//        dbSource.setId("testDbSourceId");
//        dbSource.setType("nosql");
//        dbSource.setProvider(Provider.MONGO);
//        given(dbSourceRepository.findById(projectTest.getDbsourceId())).willReturn(Optional.of(dbSource));
//
//        containerService.updateResources(projectTestNoSecurity, "testNoSecurity");
//        assertEquals(0, containerTestNoSecurity.getResourceGroups().size());
//
//        containerService.updateResources(projectTest, "test");
//        assertEquals(2, containerTest.getResourceGroups().size());
//    }
//
//    @Test
//    public void saveResource() {
//        DBSourceDto dbSource = new DBSourceDto();
//        dbSource.setId("testDbSourceId");
//        dbSource.setType("sql");
//        ResourceGroup sec = new ResourceGroup();
//        Container container = new Container();
//
//        containerService.saveResources(container, projectTest, dbSource, sec, "test@test.com");
//        assertEquals(2, container.getResourceGroups().size());
//        assertEquals("Authentication Grizzly", container.getResourceGroups().get(0).getName());
//        assertEquals("Untitled", container.getResourceGroups().get(1).getName());
//
//        ResourceGroup secAuthMS = new ResourceGroup();
//        Container containerAuthMS = new Container();
//        DBSourceDto dbSourceAuthMS = new DBSourceDto();
//        dbSource.setId("testDbSourceIdAuthMS");
//        dbSource.setType("nosql");
//        containerService.saveResources(containerAuthMS, projectTestAuthMS, dbSourceAuthMS, secAuthMS, "test@test.com");
//        assertEquals("Authentication Oauth", containerAuthMS.getResourceGroups().get(0).getName());
//        assertEquals(1, containerAuthMS.getResourceGroups().size());
//
//        ResourceGroup secNoSecurity = new ResourceGroup();
//        Container containerNoSecurity = new Container();
//        DBSourceDto dbSourceNoSecurity = new DBSourceDto();
//        containerService.saveResources(containerNoSecurity, projectTestNoSecurity, dbSourceNoSecurity, secNoSecurity, "test@test.com");
//        assertEquals("Untitled", containerNoSecurity.getResourceGroups().get(0).getName());
//        assertEquals(1, containerAuthMS.getResourceGroups().size());
//
//        assertEquals("test@test.com", container.getUserEmail());
//
//    }
//
//}

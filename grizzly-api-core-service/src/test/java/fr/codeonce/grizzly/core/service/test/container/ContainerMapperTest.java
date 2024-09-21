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

//public class ContainerMapperTest extends AbstractServiceTest {
//
//	@Autowired
//	private ContainerMapperService containerMapper;
//
//	@Autowired
//	private ContainerHierarchyRepository containerHierarchyRepository;
//
//	private ContainerHierarchy containerHierarchy;
//	private Resource resource;
//	private ResourceDto resourceDto;
//
//	@Before
//	public void init() {
//		// Container
//		containerHierarchy = new ContainerHierarchy();
//		containerHierarchy.setId("id");
//		// Resource
//		resource = new Resource();
//		ResourceFile resourceFile = new ResourceFile();
//		resourceFile.setFileId("123");
//		resourceFile.setFileUri("uri");
//		resource.setResourceFile(resourceFile);
//		// ResourceDto
//		resourceDto = new ResourceDto();
//	}
//
//	@Test
//	public void testMapContainerDtoToDomain() {
//		// INIT
//		Container cont = new Container();
//		cont.setHierarchyId("id");
//		Optional<Container> opCont = Optional.of(cont);
//		given(containerHierarchyRepository.findById("id")).willReturn(Optional.of(containerHierarchy));
//		given(containerHierarchyRepository.save(new ContainerHierarchy(""))).willReturn(containerHierarchy);
//		//given(containerRepository.findById("idCont")).willReturn(opCont);
//		// DATA
//		ContainerDto containerDto = new ContainerDto();
//		containerDto.setId("idCont");
//		containerDto.setResourceGroups(Collections.singletonList(new ResourceGroupDto(null)));
//		// TEST
//		assertTrue(containerMapper.mapToDomain(containerDto) instanceof Container);
//
//	}
//	
//	@Test
//	public void testMapResourceToDto() {
//	//	assertTrue(containerMapper.mapToDto(resource) instanceof ResourceDto);
//	}
//	
//	@Test
//	public void mapResourceToDomain() {
//		//assertTrue(containerMapper.mapToDomain(resourceDto) instanceof Resource);
//	}
//
//}

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

//public class IdentityProviderServiceTest extends AbstractServiceTest {
//
//    @Autowired
//    IdentityProviderService identityProviderService;
//
//    private List<IdentityProvider> testIdentityProviders = new ArrayList<>();
//    private List<IdentityProvider> testIdentityProviders1 = new ArrayList<>();
//    private List<IdentityProviderDto> testIdentityProvidersDto = new ArrayList<>();
//    private IdentityProvider testIdentityProvider = new IdentityProvider();
//    private IdentityProvider testIdentityProvider1 = new IdentityProvider();
//    private IdentityProviderDto testIdentityProviderDto = new IdentityProviderDto();
//    private Member member = new Member();
//    List<String> teamIds = new ArrayList<>();
//
//    MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
//    MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
//
//    private static final IdentityProviders MY_IDENTITY_PROVIDER_NAME = IdentityProviders.KEYCLOAK;
//    private static final String MY_ID = "test";
//
//    @Before
//    public void init() {
//        testIdentityProviders = new ArrayList<IdentityProvider>();
//
//        testIdentityProvider = new IdentityProvider();
//        testIdentityProvider.setId("test");
//        testIdentityProvider.setName(IdentityProviders.KEYCLOAK);
//        testIdentityProvider.setDescription("description");
//        testIdentityProviders.add(testIdentityProvider);
//
//        testIdentityProvider1 = new IdentityProvider();
//        testIdentityProvider1.setId("test1");
//        testIdentityProvider1.setName(IdentityProviders.GOOGLE);
//        testIdentityProvider1.setDescription("description1");
//        testIdentityProviders1.add(testIdentityProvider1);
//
//        testIdentityProviderDto = new IdentityProviderDto();
//        testIdentityProviderDto.setId("test");
//        testIdentityProviderDto.setName(IdentityProviders.KEYCLOAK);
//        testIdentityProviderDto.setDescription("description");
//        testIdentityProvidersDto.add(testIdentityProviderDto);
//
//        // Organization member
//        member.setEmail("test@test.com");
//        teamIds.add("teamId");
//        member.setTeamIds(teamIds);
//        // Authentication
//        SecurityContextUtil.setUpSecurityContext("test@test.com");
//
//        // Test Connection
//
//        body.add("client_id", "test");
//        body.add("client_secret", "test");
//        body.add("grant_type", "password");
//        body.add("issuer", "http://localhost:8080/auth/realms/test");
//        body.add("access_type", "public");
//
//        map.add("client_id", "undefined");
//        map.add("client_secret", "undefined");
//        map.add("grant_type", "undefined");
//        map.add("issuer", "undefined");
//        map.add("access_type", "confidential");
//
//    }
//
//    @Test
//    public void testAddIdentityProvider() throws ParseException {
//
//        // Call
//        testIdentityProvider.setUserEmail("test@test.com");
//        given(identityProviderRepository.save(any())).willReturn(testIdentityProvider);
//
//        // Test
//        IdentityProviderDto myCreatedIPDto = identityProviderService.saveIdentityProvider(testIdentityProviderDto);
//        assertEquals(MY_IDENTITY_PROVIDER_NAME, myCreatedIPDto.getName());
//    }
//
//    @Test
//    public void testAddIdentityProviderWithCurrentUsername() throws ParseException {
//        User user = new User();
//        user.setEmail("test@test.com");
//        // Call
//        given(userRepository
//                .findByEmail(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()))
//                .willReturn(Optional.of(user));
//        testIdentityProvider1.setCredentials(null);
//        given(identityProviderRepository.save(any())).willReturn(testIdentityProvider1);
//
//        // Test
//        IdentityProviderDto myCreatedIPDto = identityProviderService.saveIdentityProvider(testIdentityProviderDto);
//        assertEquals(IdentityProviders.GOOGLE, myCreatedIPDto.getName());
//    }
//
//    @Test
//    public void testDeleteIdentityProvider() {
//
//        // Mock
//        given(identityProviderRepository.findById(MY_ID)).willReturn(Optional.of(testIdentityProvider));
//
//        // Call
//        identityProviderService.deleteById(MY_ID);
//        Mockito.verify(identityProviderRepository, times(1)).deleteById(MY_ID);
//    }
//
//    @Test
//    public void testGetIdentityProvider() {
//
//        // Call
//        given(identityProviderRepository.findById(MY_ID)).willReturn(Optional.of(testIdentityProvider));
//
//        // Test
//        IdentityProviderDto myProjectDto = identityProviderService.getIdentityProviderDtoById(MY_ID);
//        assertEquals(MY_IDENTITY_PROVIDER_NAME, myProjectDto.getName());
//    }
//
//    @Test
//    public void getIdentityProviderByType() {
//        // call
//        given(identityProviderRepository.findIdentityProviderByName(IdentityProviders.KEYCLOAK))
//                .willReturn(testIdentityProviders);
//        given(identityProviderRepository.findIdentityProviderByName(IdentityProviders.GOOGLE))
//                .willReturn(testIdentityProviders1);
//
//        List<IdentityProvider> idpByTypeListKeycloak = identityProviderService.getIdentityProviderByType("keycloak");
//        List<IdentityProvider> idpByTypeListGoogle = identityProviderService.getIdentityProviderByType("google");
//        List<IdentityProvider> idpByTypeListNoIP = identityProviderService.getIdentityProviderByType("test");
//        assertEquals(testIdentityProviders, idpByTypeListKeycloak);
//        assertEquals(testIdentityProviders1, idpByTypeListGoogle);
//        assertEquals(Collections.emptyList(), idpByTypeListNoIP);
//    }
//
//    @Test
//    public void existsIdentityProviderDisplayedName() {
//        // call
//        given(identityProviderRepository.existsByDisplayedNameIgnoreCaseAndUserEmail("test", "test@test.com")).willReturn(true);
//
//        Boolean idpExists = identityProviderService.existsIdentityProviderDisplayedName("test");
//
//        assertEquals(true, idpExists);
//    }
//
//    @Test
//    public void checkRealm() throws ParseException {
//        HttpHeaders headers = new HttpHeaders();
//        HttpClientErrorException unauthorizedError = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
//                "[{\"error\": \"invalid_client\",\"error_description\": \"Missing parameter: username\"}]",
//                headers, "test".getBytes(), null);
//        HttpClientErrorException notFoundError = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not_found",
//                headers, "test".getBytes(), null);
//        boolean missingUsername = identityProviderService.checkRealm(unauthorizedError);
//        boolean notFound = identityProviderService.checkRealm(notFoundError);
//        assertEquals(true, missingUsername);
//        assertEquals(false, notFound);
//    }
//
//    @Test
//    public void isInformationsExists() {
//
//        Boolean informationExistence = identityProviderService.isInformationsExists(body);
//        Boolean informationNotExist = identityProviderService.isInformationsExists(map);
//        assertEquals(true, informationExistence);
//        assertEquals(false, informationNotExist);
//    }
//
//    @Test
//    public void checkConnection() throws ParseException, URISyntaxException {
//        boolean check = identityProviderService.checkConnection(map);
//        assertEquals(false, check);
//    }
//
//    @Test
//    public void getAllIdentityProviders() {
//        given(identityProviderRepository.findAll()).willReturn(testIdentityProviders);
//        given(identityProviderRepository.findAllByUserEmail("test@test.com")).willReturn(testIdentityProviders);
//        List<IdentityProviderDto> idpListDto = identityProviderService.getAll();
//        assertEquals(testIdentityProvidersDto.get(0).getId(), idpListDto.get(0).getId());
//    }
//
//    @Test
//    public void testgetAllIdentityProviderWithMemberOrganization() throws ParseException {
//        testIdentityProviderDto.setTeamIds(teamIds);
//        // Call
//        given(identityProviderRepository.findAll()).willReturn(testIdentityProviders);
//        given(memberRepository.findByEmail("test@test.com")).willReturn(member);
//        given(identityProviderRepository.findAllByUserEmail("test@test.com")).willReturn(testIdentityProviders);
//
//        // Test
//        List<IdentityProviderDto> idpListDto = identityProviderService.getAll();
//        assertEquals(testIdentityProvidersDto.get(0).getId(), idpListDto.get(0).getId());
//    }
//
//    @Test
//    public void deleteByNameAndUserEmail() {
//        String name = identityProviderService.deleteByNameAndUserEmail("test", "test@test.com");
//        assertEquals("test", name);
//    }
//
//    @Test
//    public void existsByNameAndUserEmail() {
//        given(identityProviderRepository.existsByNameAndUserEmail("test", "test@test.com")).willReturn(true);
//        boolean exists = identityProviderService.existsByNameAndUserEmail("test", "test@test.com");
//        boolean notExists = identityProviderService.existsByNameAndUserEmail("test1", "test1@test.com");
//        assertEquals(true, exists);
//        assertEquals(false, notExists);
//
//    }
//
//    @Test
//    public void getIdentityProviderEntity() {
//        given(identityProviderRepository.findById("test")).willReturn(Optional.of(testIdentityProvider));
//        IdentityProvider idp = identityProviderService.getIdentityProviderEntity("test");
//        assertEquals(testIdentityProvider, idp);
//    }
//}

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

//public class UserServiceTest extends AbstractServiceTest {
//
//    private static final String MY_FIRSTNAME = "MY_FIRSTNAME";
//    private static final String MY_LASTNAME = "MY_LASTNAME";
//    private static final String MY_USER_EMAIL = "myEmail";
//    private static final String MY_PASSWORD = "myPass";
//
//    @Autowired
//    private UserService userService;
//
//    private List<User> testUsers = new ArrayList<>();
//    private User testUser = new User();
//    private UserDto testUserDto = new UserDto();
//
//    @MockBean
//    private ProjectExample projectExample;
//
//
//    @Before
//    public void init() {
//        testUsers = new ArrayList<User>();
//        testUser = new User();
//        testUser.setEmail(MY_USER_EMAIL);
//        testUser.setFirstName(MY_FIRSTNAME);
//        testUser.setLastName(MY_LASTNAME);
//        testUser.setPassword(MY_PASSWORD);
//        testUsers.add(testUser);
//
//        testUserDto = new UserDto();
//        testUserDto.setEmail(MY_USER_EMAIL);
//        testUser.setFirstName(MY_FIRSTNAME);
//        testUser.setLastName(MY_LASTNAME);
//        testUserDto.setPassword(MY_PASSWORD);
//
//    }
//
////    @Test
////    public void testGetAllUsers() {
////
////        // Call
////        given(userRepository.findAll()).willReturn(testUsers);
////
////        // Test
////        List<UserDto> allUsers = userService.getAllUsers();
////        assertFalse(allUsers.isEmpty());
////        assertEquals(MY_USER_EMAIL, testUser.getEmail());
////    }
//
//    // LOGIN
////
////	@Test
////	public void testGetUser() {
////		// Call
////		given(userRepository.findByEmail(MY_USER_EMAIL)).willReturn(Optional.of(testUser));
////
////		// Test
////
////		String token = userService.login(MY_USER_EMAIL, MY_PASSWORD);
////		Assert.assertFalse(token == null);
////
////	}
////
////	@Test(expected = HttpClientErrorException.class)
////	public void testGetNotFoundUser() {
////
////		// Mock
////		given(userRepository.findByEmail(MY_USER_EMAIL)).willReturn(Optional.empty());
////
////		// Test
////		 userService.login(MY_USER_EMAIL, MY_PASSWORD);
////	}
//
//    // SIGNUP
////    @Test
////    public void testAddUser() throws IOException {
////
////        // Call
////        given(userRepository.save(any())).willReturn(testUser);
////        // Test
////        UserDto myCreatedUserDto = userService.addUser(testUserDto);
////        assertFalse(myCreatedUserDto == null);
////        assertEquals(MY_USER_EMAIL, myCreatedUserDto.getEmail());
////
////    }
//
////    @Test
////    public void testExistsUser() {
////        // Mock
////        given(userRepository.existsByEmailIgnoreCase(MY_USER_EMAIL)).willReturn(true);
////
////        // Call
////        boolean userTaken = userService.existsByEmail(MY_USER_EMAIL);
////        assertTrue(userTaken);
////
////    }
//
//}

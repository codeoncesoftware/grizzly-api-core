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
package fr.codeonce.grizzly.core.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.auth.AuthService;
import fr.codeonce.grizzly.core.service.auth.GithubAuthService;
import fr.codeonce.grizzly.core.service.auth.GoogleAuthService;
import fr.codeonce.grizzly.core.service.auth.LoginDto;
import fr.codeonce.grizzly.core.service.container.ContainerDto;
import fr.codeonce.grizzly.core.service.container.ContainerService;
import fr.codeonce.grizzly.core.service.container.IntegrationDto;
import fr.codeonce.grizzly.core.service.project.ProjectExample;
import fr.codeonce.grizzly.core.service.security.AuthResponse;
import fr.codeonce.grizzly.core.service.security.JWTGenerator;
import fr.codeonce.grizzly.core.service.user.UserDto;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import fr.codeonce.grizzly.core.service.util.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(
            AuthController.class
    );

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerService containerService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    // @Lazy
    @Autowired
    private GithubAuthService githubAuthService;
    
    @Autowired
    private GoogleAuthService googleAuthService;

    // TODO : change when apikey ready
    @Autowired
    private ProjectExample projectExample;

    @Autowired
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTGenerator jwtGenerator;

    @Autowired
    private EmailService emailService;

    /**
     * Authenticate the user using email and password
     *
     * @param login
     * @return token
     * @throws IOException
     */
    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginDto login,
            HttpServletRequest req
    ) throws IOException {
        return authService.login(login.getEmail(), login.getPassword());
    }

    /**
     * Save a new User after Registration
     *
     * @param userDto
     * @param req
     * @return
     * @throws IOException
     */
    @PostMapping("/signup")
    public UserDto signup(@RequestBody UserDto userDto, HttpServletRequest req)
            throws IOException {
        log.info("request signup {}", userDto.getEmail());
        log.info("Lang : {}", req.getHeader("Accept-Language"));
        UserDto user = userService.addUser(userDto);
        userService.confirmRegistration(
                user.getEmail(),
                req.getHeader("Accept-Language")
        );
        return user;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/github/login")
    public AuthResponse githubLogin(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IllegalAccessException, IOException {
        return githubAuthService.loginWithGithub(code);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/google/login")
    public AuthResponse googleLogin(
            @RequestParam String code,
            @RequestParam String scope,
            @RequestParam String authuser,
            @RequestParam String prompt,
            HttpServletResponse response
    ) throws IllegalAccessException {
        return googleAuthService.loginWithGoogle(code);
    }


    @GetMapping("/logout")
    public void logout(HttpServletRequest req) {
        log.info("request logout");
        userService.logout(req);
    }

    /**
     * Check the Email's Unicity
     *
     * @param email
     * @param req
     * @return
     */
    @GetMapping("/check/{email}")
    public boolean existsByEmail(
            @PathVariable String email,
            HttpServletRequest req
    ) {
        log.info("request check email: {}", email);
        return userService.existsByEmail(email);
    }

    /**
     * Verify the received Token and enable the associated user email
     *
     * @param token
     * @param res
     * @throws IOException
     */
    @GetMapping("/confirm/email/{token}")
    public void confirmEmail(@PathVariable String token, HttpServletResponse res)
            throws IOException {
        log.info("request to confirm email token received");
        userService.confirmEmail(token);
    }

    @GetMapping("/checknewsletter/{userEmail}")
    public void updateLastconnection(
            @PathVariable String userEmail,
            HttpServletResponse res
    ) throws IOException {
        log.info("request to check newsletter received");
        userService.checkNewsletter(userEmail, true);
    }

    /**
     * @param token
     * @param password
     **/
    @PostMapping("/reset/password/{token}")
    public void resetPassword(
            @PathVariable String token,
            @RequestBody Map<String, String> password
    ) {
        log.info("request to reset password received");
        userService.resetPassword(token, password.get("password"));
    }

    /**
     * Send an email to the user to grant him access to the reset password page
     *
     * @param email
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @GetMapping("/send/reset/password/{email}")
    public void sendResetPassword(
            @PathVariable String email,
            HttpServletRequest req
    ) {
        log.info("request to send reset password email for {} ", email);
        userService.sendResetPassword(email, req.getHeader("Accept-Language"));
    }

    /**
     * Update Apikey users if not exist
     */
    @GetMapping("/updateApikey")
    public void updateApiKey() {
        log.info("request to Update ApiKey if not exist");
        List<UserDto> allUsers = userService.getAllUsers();
        allUsers
                .stream()
                .forEach(c -> {
                    if (c.getApiKey() == null) userService.updateUserApiKey(c);
                });
    }

    /**
     * Checks the Token Validity
     *
     * @param token
     * @return
     */
    @GetMapping("/check/token/{token}")
    public boolean checkToken(@PathVariable String token) {
        log.info("request to check Token validaty");
        return userService.checkToken(token);
    }

    @PostMapping("/editor")
    public UserDto addEditor() throws IOException {
        return userService.addEditor();
    }

    @PostMapping("/editor/example")
    public ContainerDto editorProjectExample() throws Exception {
        return projectExample.editorProjectExample();
    }

    @GetMapping("/userapikey")
    public boolean getUserByApiKey(
            @RequestParam(required = false) String apikey,
            @RequestParam String containerId
    ) {
        log.info("apikey {}, containerId {}", apikey, containerId);
        if (apikey != null) {
            log.info("Request to fetch user with email : {}", apikey);
            if (
                    userService.getUserByApiKey(apikey, containerId)
            ) return true;
            else return false;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "apikey not found"
            );
        }
    }

    @PostMapping("/integrate")
    public boolean integrate(@RequestBody IntegrationDto integrationDto)
            throws ParseException, CustomGitAPIException, SQLException {
        return containerService.integrate(
                integrationDto.getEmail(),
                integrationDto.getContainerId()
        );
    }

    @GetMapping("/reset/date")
    public void resetDate()
            throws ParseException, CustomGitAPIException, SQLException {
        userRepository
                .findAll()
                .forEach(user -> {
                    user.setRegistrationDate(new Date());
                    userRepository.save(user);
                });
    }
}

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
package fr.codeonce.grizzly.core.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.security.AuthResponse;
import fr.codeonce.grizzly.core.service.security.JWTGenerator;
import fr.codeonce.grizzly.core.service.user.UserDto;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private GrizzlyCoreProperties properties;

    @Lazy
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Value("${server.port}")
    private String port;

    private static final String EMAIL = "email";

    @Autowired
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JWTGenerator jwtGenerator;

    /**
     * Authenticate the user using email and password
     *
     * @param email
     * @param password
     * @return token
     */
    public AuthResponse login(String email, String password) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        DigestUtils.sha256Hex(password + DigestUtils.sha256Hex("co%de01/"))));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerator.generateToken(authentication);
        return new AuthResponse(token, email);

    }

    protected String prepareRestTemplate(String url, String email, String loginResEmail) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("url construction");
        RestTemplate restTamplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> result;

        stopWatch.stop();

        try {
            stopWatch.start("The login process");
            result = restTamplate.exchange(url, HttpMethod.POST, request, String.class);
            stopWatch.stop();

            SecurityContextUtil.setUpSecurityContext(email);
            UserDto currentUser = userService.getUser(email);
            userService.updateUser(currentUser);
            if (currentUser.isFirstTime()) {
                currentUser.setFirstTime(false);
                currentUser.setPassword(null);
                userService.updateUser(currentUser);
            }
            @SuppressWarnings("unchecked")
            Map<String, String> loginRes = new ObjectMapper().readValue(result.getBody(), Map.class);
            loginRes.put(EMAIL, loginResEmail);
            return new ObjectMapper().writeValueAsString(loginRes);

        } catch (HttpClientErrorException.BadRequest e) {
            String responseBodyAsString = e.getResponseBodyAsString();
            if (responseBodyAsString.contains("Bad credentials")) {
                throw new BadCredentialsException("4011"); // Code 1: invalid credentials
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create Project Example");
        }

        throw new IllegalStateException("Error in login");
    }

    private String prepareToken(String email, String password) {
        String clientId = this.properties.getOauth2().getClientId();
        String clientSecret = this.properties.getOauth2().getClientSecret();
        String grantType = this.properties.getOauth2().getGrantType();
        String jwtKey = this.properties.getOauth2().getJwtKey();

        return "http://localhost:" + port + "/oauth/token?client_id=" + clientId + "&client_secret=" + clientSecret
                + "&grant_type=" + grantType + "&username=" + email + "&password="
                + DigestUtils.sha256Hex(password + DigestUtils.sha256Hex("co%de01/")) + "&jwt_key=" + jwtKey;

    }

}

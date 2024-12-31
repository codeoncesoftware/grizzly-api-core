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

import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.security.AuthResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final String googleClientId;
    private final String googleClientSecret;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final String googleAccessTokenUri;
    private final String googleUserInfoUri;
    private final String frontUrl;

    public GoogleAuthService(
            @Value("${google.client.clientId}") String googleClientId,
            @Value("${google.client.clientSecret}") String googleClientSecret,
            UserRepository userRepository,
            AuthService authService,
            @Value("${google.client.accessTokenUri}") String googleAccessTokenUri,
            @Value("${google.resource.userInfoUri}") String googleUserInfoUri,
            @Value("${frontUrl}") String frontUrl
    ) {
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.userRepository = userRepository;
        this.authService = authService;
        this.googleAccessTokenUri = googleAccessTokenUri;
        this.googleUserInfoUri = googleUserInfoUri;
        this.frontUrl = frontUrl;
    }

    public AuthResponse loginWithGoogle(String code) throws IllegalAccessException {
        log.info("Start Login with google process");
        RestTemplate restTemplate = new RestTemplate();
        var googleTokenUriResponse = getGoogleTokenUriResponse(code, restTemplate);

        if (googleTokenUriResponse.getBody() != null) {
            Map<String, String> googleUserinfo = getGoogleUserinfo(restTemplate, googleTokenUriResponse);
            String email = googleUserinfo.get("email");
            String firstName = googleUserinfo.get("given_name");
            String lastName = googleUserinfo.get("family_name");
            String phoneNumber = googleUserinfo.get("phone_number");
            Optional<User> opUser = userRepository.findByEmail(email);
            if (opUser.isEmpty()) {
                createNewGoogleUser(firstName, lastName, phoneNumber, email);
                return loginGoogle(email);
            } else {
                return loginGoogle(email);
            }
        } else {
            log.error("Error Occurred while generating google user token");
            throw new IllegalAccessException("Cannot access Google Account");
        }
    }

    private Map<String, String> getGoogleUserinfo(
            RestTemplate restTemplate,
            ResponseEntity<Map> googleTokenUriResponse
    ) {
        log.info("Start generating google user info");
        return restTemplate
                .getForObject(googleUserInfoUri + "?access_token=" + googleTokenUriResponse.getBody().get("access_token"),
                Map.class);
    }

    private ResponseEntity<Map> getGoogleTokenUriResponse(String code, RestTemplate restTemplate) {
        log.info("Start generating google token");
        UriComponentsBuilder accessTokenCall = UriComponentsBuilder.fromUriString(googleAccessTokenUri)
                                                                   .queryParam("code", code)
                                                                   .queryParam("grant_type", "authorization_code")
                                                                   .queryParam("redirect_uri",frontUrl + "/google/login")
                                                                   .queryParam("client_id", googleClientId)
                                                                   .queryParam("client_secret", googleClientSecret);
	    return restTemplate.exchange(accessTokenCall.toUriString(), HttpMethod.POST, null, Map.class);
    }


    public AuthResponse loginGoogle(String email) {
        Optional<User> opUser = this.userRepository.findByEmail(email);
        if (opUser.isPresent() && !opUser.get().isEnabled()) {
            throw new BadCredentialsException("4012"); // Code 2: account not activated
        }
        return authService.login(email, email);
    }


    @SuppressWarnings("unchecked")
    private Optional<User> createNewGoogleUser(String firstName, String lastName, String phoneNumber, String email) {
        User googleUser = new User();
        googleUser.setFirstName(firstName);
        googleUser.setLastName(lastName);
        googleUser.setEmail(email);
        googleUser.setPhone(phoneNumber);
        googleUser.setApiKey(RandomStringUtils.randomAlphabetic(32));
        googleUser.setPassword(DigestUtils.sha256Hex(email + DigestUtils.sha256Hex("co%de01/")));
        googleUser.setEnabled(true);
        googleUser.setFirstTime(true);
        googleUser.setLastConnection(new Date());
        User user = userRepository.save(googleUser);
        return userRepository.findById(user.getId());
    }

}

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.security.AuthResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

// @Profile({ "!local" })
@Service
public class GithubAuthService {

    private static final Logger log = LoggerFactory.getLogger(GithubAuthService.class);

    @Autowired
    private GrizzlyCoreProperties properties;

    @Value("${github.client.clientId}")
    private String githubClientId;

    @Value("${github.client.clientSecret}")
    private String githubClientSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Value("${github.client.accessTokenUri}")
    private String githubAccessTokenUri;

    @Value("${github.resource.userInfoUri}")
    private String githubUserInfoUri;

    @Value("${github.resource.emailInfoUri}")
    private String githubEmailInfo;

    @Value("${github.resource.salt}")
    private String githubSalt;

    @Value("${server.port}")
    private String port;

    private static final String ACCESS_TOKEN = "access_token";

    public AuthResponse loginWithGithub(String code) throws IllegalAccessException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder accessTokenCall = UriComponentsBuilder.fromUriString(githubAccessTokenUri)
                .queryParam("code", code)
                .queryParam("client_id", githubClientId)
                .queryParam("client_secret", githubClientSecret);
        var res = restTemplate.exchange(accessTokenCall.toUriString(), HttpMethod.POST, null, Map.class);

        if (ObjectUtils.allNotNull(res.getBody().get(ACCESS_TOKEN))) {
            UriComponentsBuilder emailInfoCall = UriComponentsBuilder.fromUriString(githubEmailInfo);
            UriComponentsBuilder userInfoCall = UriComponentsBuilder.fromUriString(githubUserInfoUri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + res.getBody().get(ACCESS_TOKEN)); // accessToken can be the secret
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> tokenEntity = new HttpEntity<>(headers);

            var emails = restTemplate.exchange(emailInfoCall.toUriString(), HttpMethod.GET, tokenEntity, JsonNode.class);
            var user = restTemplate.exchange(userInfoCall.toUriString(), HttpMethod.GET, tokenEntity, JsonNode.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            JsonNode emailBody = jsonWriter.readTree(emails.getBody().toString());
            JsonNode userBody = jsonWriter.readTree(user.getBody().toString());
            String email = emailBody.get(0).get("email").asText();
            String login = userBody.get("login").asText();
            Optional<User> opUser = userRepository.findByEmail(email);
            if (opUser.isEmpty()) {
                createNewGithubUser(login, email);
                return loginGithub(email);
            } else {
                return loginGithub(email);
            }

        } else {
            throw new IllegalAccessException("Cannot access Github Account");
        }
    }


    public AuthResponse loginGithub(String email) {
        Optional<User> opUser = this.userRepository.findByEmail(email);
        if (opUser.isPresent() && !opUser.get().isEnabled()) {
            throw new BadCredentialsException("4012"); // Code 2: account not activated
        }
        return authService.login(email, email);
    }


    @SuppressWarnings("unchecked")
    private Optional<User> createNewGithubUser(String login, String email) {
        User githubUser = new User();
        githubUser.setFirstName(login);
        githubUser.setLastName(login);
        githubUser.setEmail(email);
        githubUser.setPhone("");
        githubUser.setApiKey(RandomStringUtils.randomAlphabetic(32));
        githubUser.setPassword(DigestUtils.sha256Hex(email + DigestUtils.sha256Hex("co%de01/")));
        githubUser.setEnabled(true);
        githubUser.setFirstTime(true);
        githubUser.setLastConnection(new Date());
        User user = userRepository.save(githubUser);
        return userRepository.findById(user.getId());
    }

}

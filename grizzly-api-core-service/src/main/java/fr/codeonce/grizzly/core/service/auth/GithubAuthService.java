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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;

@Profile({ "!local" })
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

	@Value("${github.resource.salt}")
	private String githubSalt;
	
    @Value("${server.port}")
	private String port;

	private static final String ACCESS_TOKEN = "access_token"; 
	private static final String EMAIL = "email"; 

    @SuppressWarnings("unchecked")
	public String loginWithGithub(String code, String product)
			throws IllegalAccessException, IOException {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, String> args = new HashMap<>();
		args.put("code", code);
		if (product.equals("grizzly_api")) {
			args.put("client_id", githubClientId);
			args.put("client_secret", githubClientSecret);
		}
		Map<String, String> res = restTemplate.postForObject(githubAccessTokenUri, args, Map.class);
		if (ObjectUtils.allNotNull(res.get(ACCESS_TOKEN))) {
			ObjectMapper jsonMapper = new ObjectMapper();
			String content = IOUtils.toString(
					new URL(githubUserInfoUri + "/emails?access_token=" + res.get(ACCESS_TOKEN)),StandardCharsets.UTF_8);
			String email = "";
			if (!content.isEmpty()) {
				Object obj = jsonMapper.readValue(content, Object.class);
				ObjectMapper jsonWriter = new ObjectMapper();
				String cont = jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonBody = mapper.readTree(cont);
				email = jsonBody.get(0).findValue(EMAIL).asText();
			}
			Map<String, String> res1 = restTemplate
					.getForObject(githubUserInfoUri + "?access_token=" + res.get(ACCESS_TOKEN), Map.class);
			Optional<User> opUser = userRepository.findByEmail(email);
			if (!opUser.isPresent()) {
				createNewGithubUser(res1, email);
				return authService.login(email, githubSalt + email);
			} else {
				log.info("connected");
				return loginGithub(email, opUser.get().getPassword());
			}

		} else {
			throw new IllegalAccessException("Cannot access Github Account");
		}

	}

    
	public String loginGithub(String email, String password) {
		Optional<User> opUser = this.userRepository.findByEmail(email);
		if (opUser.isPresent() && !opUser.get().isEnabled()) {
			throw new BadCredentialsException("4012"); // Code 2: account not activated
		}
		String url = prepareTokenGithub(email, password);
		return authService.prepareRestTemplate(url, email , email);

	}

	private String prepareTokenGithub(String email, String password) {
		String clientId = this.properties.getOauth2().getClientId();
		String clientSecret = this.properties.getOauth2().getClientSecret();
		String grantType = this.properties.getOauth2().getGrantType();
		String jwtKey = this.properties.getOauth2().getJwtKey();

		return "http://localhost:" + port + "/oauth/token?client_id=" + clientId + "&client_secret=" + clientSecret
				+ "&grant_type=" + grantType + "&username=" + email + "&password=" + password + "&jwt_key=" + jwtKey;

	}

	@SuppressWarnings("unchecked")
	private Optional<User> createNewGithubUser(Map<String, String> res, String email) {
		User githubUser = new User();
		githubUser.setFirstName(res.get("login"));
		githubUser.setLastName(res.get("login"));
		githubUser.setEmail(email);
		githubUser.setPhone("");
		githubUser.setApiKey(RandomStringUtils.randomAlphabetic(32));
		githubUser.setPassword(DigestUtils.sha256Hex(githubSalt + email + DigestUtils.sha256Hex("co%de01/")));
		githubUser.setEnabled(true);
		githubUser.setFirstTime(true);
		githubUser.setLastConnection(new Date());
		User user = userRepository.save(githubUser);
		return userRepository.findById(user.getId());
	}

}

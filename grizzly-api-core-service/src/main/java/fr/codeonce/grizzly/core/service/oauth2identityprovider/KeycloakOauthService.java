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
package fr.codeonce.grizzly.core.service.oauth2identityprovider;

import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProvider;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import fr.codeonce.grizzly.core.service.util.IdentityProviderUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;

@Service
public class KeycloakOauthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakOauthService.class);

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    private IdentityProviderUtils idpUtils = new IdentityProviderUtils();

    public Map<String, Object> authorizationEndpoint(String username, String password, String containerId)
            throws ParseException {
        Container container = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));

        Project project = projectRepository.findById(container.getProjectId())
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, container.getProjectId()));
        Map<String, Object> outputMsg = new HashMap<>();
        log.info("login with keycloak");
        if (!project.getIdentityProviderIds().isEmpty()) {
            StringBuilder idpID = getActualIdentityProviderId(project);

            outputMsg = outputAuthMsg(username, password, idpID);
        }
        return outputMsg;

    }

    private StringBuilder getActualIdentityProviderId(Project project) {
        StringBuilder idpID = new StringBuilder();

        project.getIdentityProviderIds().stream().forEach(ipID -> {
            if (identityProviderRepository.findById(ipID).get().getName().equals(IdentityProviders.KEYCLOAK)) {
                idpID.append(ipID);
            }
        });
        return idpID;
    }

    private Map<String, Object> outputAuthMsg(String username, String password, StringBuilder idpID)
            throws ParseException {
        Map<String, Object> outputMsg = new HashMap<>();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();
        IdentityProvider identityP = identityProviderRepository.findById(idpID.toString())
                .orElseThrow(GlobalExceptionUtil.notFoundException(IdentityProvider.class, idpID.toString()));
        log.info("extract keycloak userinfo");

        body.add("username", username);
        body.add("password", password);
        body.add("client_id", identityP.getCredentials().getClientId());
        body.add("client_secret", identityP.getCredentials().getSecretKey());
        body.add("grant_type", identityP.getCredentials().getGrantType());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

            String tokenData = restTemplate.exchange(
                    identityP.getCredentials().getIssuer() + "/protocol/openid-connect/token", HttpMethod.POST,
                    entity,
                    String.class).getBody();
            JSONObject json = idpUtils.parser(tokenData);
            JSONObject tokenInfo = idpUtils.parser(JwtHelper.decode(json.get("access_token").toString()).getClaims());
            JSONObject resourceAccess = idpUtils.parser(tokenInfo.get("resource_access").toString());

            JSONObject clientInfo = (JSONObject) resourceAccess.get(identityP.getCredentials().getClientId());
            List<String> roles = new ArrayList<>();
            if (clientInfo != null) {
                roles = Arrays.asList(clientInfo.get("roles").toString().replace("[", "").replace("]", "")
                        .replace(" ", "").replace("\"", "").split(","));
            }
            outputMsg.put("authenticationMsg", "Authentication succeeded");
            outputMsg.put("roles", roles);

            outputMsg.put("email_verified", tokenInfo.get("email_verified"));
            if (tokenInfo.get("given_name") != null) {
                outputMsg.put("given_name", tokenInfo.get("given_name").toString());
            }
            if (tokenInfo.get("family_name") != null) {
                outputMsg.put("family_name", tokenInfo.get("family_name").toString());
            }
            if (tokenInfo.get("email") != null) {
                outputMsg.put("email", tokenInfo.get("email").toString());
            }
            if (tokenInfo.get("name") != null) {
                outputMsg.put("name", tokenInfo.get("name").toString());
            }
            log.info("keycloak userinfo extracted");

        } catch (HttpClientErrorException httpClientErrorException) {
            log.info("extract keycloak userinfo exception");
            outputMsg.put("authenticationMsg", httpClientErrorException.getMessage());
        } catch (ResourceAccessException resourceAccessException) {
            outputMsg.put("authenticationMsg", "Check your keycloak server !");
        }

        return outputMsg;
    }

}

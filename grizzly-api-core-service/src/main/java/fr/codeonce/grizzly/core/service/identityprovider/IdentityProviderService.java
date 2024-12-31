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
package fr.codeonce.grizzly.core.service.identityprovider;

import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProvider;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class IdentityProviderService {

    private static final String UNDEFINED = "undefined";
    private static final String ISSUER = "issuer";
    private static final String ACCESS_TYPE = "access_type";

    @Autowired
    private IdentityProviderRepository identityproviderRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberRepository memberRepository;

    public IdentityProviderDto saveIdentityProvider(IdentityProviderDto dto) {
        String currentUserEmail = "";
        if (SecurityContextUtil.getCurrentUserEmail() != null) {
            currentUserEmail = SecurityContextUtil.getCurrentUserEmail();
            if (!currentUserEmail.contains("@")) {
                User u = userService.getConnectedUserByApiKey();
                currentUserEmail = u.getEmail();
            }
            dto.setUserEmail(currentUserEmail);

        }
        if (!dto.userEmail.isEmpty()) {
            dto.setUserEmail(currentUserEmail);
        }
        if (!dto.name.equals(IdentityProviders.KEYCLOAK)) {
            dto.setCredentials(null);
        }
        IdentityProvider identityProvider = new IdentityProvider();
        BeanUtils.copyProperties(dto, identityProvider);
        IdentityProvider storedIdentityProvider = identityproviderRepository.save(identityProvider);
        IdentityProviderDto returnedValue = new IdentityProviderDto();
        BeanUtils.copyProperties(storedIdentityProvider, returnedValue);
        return returnedValue;
    }

    public List<IdentityProviderDto> getAll() {

        String currentUserEmail = userService.getConnectedUserEmail();
        List<IdentityProvider> allIdentityProviders = identityproviderRepository.findAll();
        List<IdentityProvider> identityProviders = new ArrayList<>();
        List<IdentityProvider> personnalIdentityProviders = identityproviderRepository
                .findAllByUserEmail(currentUserEmail);

        // check if the current user has any team (a part of organization)
        if (memberRepository.findByEmail(currentUserEmail) != null) {
            List<String> memberTeams = memberRepository.findByEmail(currentUserEmail).getTeamIds();

            IntStream.range(0, memberTeams.size())
                    .mapToObj(
                            i -> IntStream.range(0, allIdentityProviders.size())
                                    .filter(j -> allIdentityProviders.get(j).getTeamIds().contains(memberTeams.get(i)))
                                    .mapToObj(j -> allIdentityProviders.get(j)))
                    .flatMap(Function.identity())
                    .collect(Collectors.toCollection(() -> identityProviders));

            personnalIdentityProviders.forEach(db -> {
                if (!containsName(identityProviders, db.getDisplayedName())) {
                    identityProviders.add(db);
                }
            });

            IntStream.range(0, identityProviders.size())
                    .forEach(i -> {
                        IdentityProvider identityProvider = identityProviders.get(i);
                        identityProvider
                                .setOrganizationId(memberRepository.findByEmail(currentUserEmail).getOrganisationId());
                        identityproviderRepository.save(identityProvider);
                    });

            return identityProviders.stream().map(db -> {

                IdentityProviderDto returnedValue = new IdentityProviderDto();
                BeanUtils.copyProperties(db, returnedValue);
                returnedValue.setActive(true);
                return returnedValue;

            }).collect(Collectors.toList());
        } else {
            IntStream.range(0, personnalIdentityProviders.size())
                    .forEach(i -> {
                        IdentityProvider identityProvider = personnalIdentityProviders.get(i);
                        identityProvider.setOrganizationId(null);
                        identityproviderRepository.save(identityProvider);
                    });

            return personnalIdentityProviders.stream().map(db -> {

                IdentityProviderDto returnedValue = new IdentityProviderDto();
                BeanUtils.copyProperties(db, returnedValue);
                returnedValue.setActive(true);
                return returnedValue;

            }).collect(Collectors.toList());
        }

    }

    private boolean containsName(final List<IdentityProvider> list, final String name) {
        return list.stream().map(IdentityProvider::getDisplayedName).anyMatch(db -> db.equalsIgnoreCase(name));
    }

    public void deleteById(String identityproviderId) {
        identityproviderRepository.deleteById(identityproviderId);
    }

    public String deleteByNameAndUserEmail(String name, String userEmail) {
        identityproviderRepository.deleteByNameAndUserEmail(name, userEmail);
        return name;
    }

    public IdentityProviderDto getIdentityProviderDtoById(String identityproviderId) {
        return identityproviderRepository.findById(identityproviderId).map(db -> {
            IdentityProviderDto returnedValue = new IdentityProviderDto();
            BeanUtils.copyProperties(db, returnedValue);
            returnedValue.setActive(true);
            return returnedValue;
        }).orElseThrow();
    }

    public List<IdentityProvider> getIdentityProviderByType(String identityProviderType) {
        if (identityProviderType.equalsIgnoreCase("keycloak")) {
            return identityproviderRepository.findIdentityProviderByName(IdentityProviders.KEYCLOAK);
        } else if (identityProviderType.equalsIgnoreCase("google")) {
            return identityproviderRepository.findIdentityProviderByName(IdentityProviders.GOOGLE);
        } else if (identityProviderType.equalsIgnoreCase("github")) {
            return identityproviderRepository.findIdentityProviderByName(IdentityProviders.GITHUB);
        } else if (identityProviderType.equalsIgnoreCase("facebook")) {
            return identityproviderRepository.findIdentityProviderByName(IdentityProviders.FACEBOOK);
        } else if (identityProviderType.equalsIgnoreCase("gitlab")) {
            return identityproviderRepository.findIdentityProviderByName(IdentityProviders.GITLAB);
        } else {
            return Collections.emptyList();
        }
    }

    public IdentityProvider getIdentityProviderEntity(String id) {
        return identityproviderRepository.findById(id).orElseThrow();
    }

    public Boolean existsByNameAndUserEmail(String name, String userEmail) {
        return identityproviderRepository.existsByNameAndUserEmail(name, userEmail);
    }


    public boolean checkUnicity(String name, String useremail, String id) {
        boolean[] exists = new boolean[]{true};

        identityproviderRepository.findIdentityProviderByDisplayedNameAndUserEmail(name, useremail).ifPresent(ip -> {

            if (!(!StringUtils.isBlank(id) && id.equalsIgnoreCase(ip.getId()))) {
                exists[0] = false;
            }
        });
        return exists[0];

    }

    public Boolean checkConnection(MultiValueMap<String, String> body) throws ParseException {
        Boolean result = false;

        if (!isInformationsExists(body)) {
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.exchange(
                    body.get(ISSUER).get(0) + "/protocol/openid-connect/token",
                    HttpMethod.POST, entity, String.class);

            return true;
        } catch (HttpClientErrorException httpClientErrorException) {

            // Chek the acess type of our keycloak client 403 client not allowed (if client public and he chose confidential)
            if (body.get(ACCESS_TYPE).get(0).contentEquals("confidential")) {
                try {
                    restTemplate.exchange(
                            body.get(ISSUER).get(0) + "/protocol/openid-connect/token/introspect",
                            HttpMethod.POST, entity, String.class);
                } catch (HttpClientErrorException httpClientException) {
                    if (httpClientException.getStatusCode().value() == 403) {
                        return false;
                    }
                }
            }

            // We got 404 exception if realm does not exist or port not acceptable
            result = checkRealm(httpClientErrorException);
        } catch (ResourceAccessException resourceAccessException) {
            return false;
        }
        return result;

    }

    public boolean isInformationsExists(MultiValueMap<String, String> body) {
        Boolean res = true;
        if ((body.get("client_id").get(0).contentEquals(UNDEFINED))
                || (body.get("client_secret").get(0).contentEquals(UNDEFINED)
                && body.get(ACCESS_TYPE).get(0).contentEquals("confidential"))
                || (body.get("grant_type").get(0).contentEquals(UNDEFINED))
                || (body.get(ISSUER).get(0).contentEquals(UNDEFINED))
                || (body.get(ACCESS_TYPE).get(0).contentEquals(UNDEFINED))) {
            res = false;
        }
        return res;
    }

    public boolean checkRealm(HttpClientErrorException httpClientErrorException) throws ParseException {
        Boolean result = false;
        if (httpClientErrorException.getStatusCode().value() != 404) {

            String msg = httpClientErrorException.getMessage();
            JSONParser parser = new JSONParser();
            if (msg != null) {
                if (msg.indexOf('{') != -1 && msg.indexOf(']') != -1 && msg.indexOf('{') < msg.indexOf(']')) {
                    JSONObject json = (JSONObject) parser.parse(msg.substring(msg.indexOf('{'), msg.indexOf(']')));
                    String invalidCredentials = json.get("error_description").toString();

                    if (httpClientErrorException.getStatusCode().value() == 401
                            && invalidCredentials.contentEquals("Missing parameter: username")) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }


    public boolean existsIdentityProviderDisplayedName(String identityProviderName) {
        String currentUserEmail = userService.getConnectedUserEmail();
        return this.identityproviderRepository.existsByDisplayedNameIgnoreCaseAndUserEmail(identityProviderName, currentUserEmail);
    }

}

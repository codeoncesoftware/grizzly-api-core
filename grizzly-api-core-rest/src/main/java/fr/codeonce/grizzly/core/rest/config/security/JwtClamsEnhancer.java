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
package fr.codeonce.grizzly.core.rest.config.security;


import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@Component
public class JwtClamsEnhancer implements TokenEnhancer {
    private final ClientDetailsService clientDetailsService;
    
    @Value("${issuer-url}")
	private String issuer;

    public JwtClamsEnhancer(ClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        final UriComponents URIComponent = ServletUriComponentsBuilder.fromCurrentRequest().build();
        //System.out.println();
        final Map<String, Object> additionalInformation = new LinkedHashMap<>();
        final Instant expiration = accessToken.getExpiration().toInstant();

        final Authentication client = SecurityContextHolder.getContext().getAuthentication();
        final String clientId = client.getName();
        final ClientDetails clientDetails = this.clientDetailsService.loadClientByClientId(clientId);

        additionalInformation.put(JwtClaimNames.ISS, issuer);
        additionalInformation.put(JwtClaimNames.EXP, expiration.getEpochSecond());
        additionalInformation.put(JwtClaimNames.IAT, expiration.minusSeconds(clientDetails.getAccessTokenValiditySeconds()).getEpochSecond());
        
        final String nonce = authentication.getOAuth2Request().getRequestParameters().get((OidcParameterNames.NONCE));
        if (nonce != null) {
            additionalInformation.put(OidcParameterNames.NONCE, nonce);
        }
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInformation);
        return accessToken;
    }
}

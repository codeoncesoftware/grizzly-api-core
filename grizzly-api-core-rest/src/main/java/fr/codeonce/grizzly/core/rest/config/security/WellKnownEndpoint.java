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

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WellKnownEndpoint {
    private final KeyPair keyPair;
    @Value("${issuer-url}")
	private String issuer;
    
    @Value("${authorization_endpoint}")
	private String authorization_endpoint;
    
    @Value("${token_keys}")
	private String token_keys;
    public WellKnownEndpoint(JwtProperties props) {
        this.keyPair = props.getKeyPair(props.getPrivatekey(), props.getPublickey());
    }

    /**
     * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig
     */
    @GetMapping(path = {".well-known/openid-configuration", "oauth/token/.well-known/openid-configuration"})
    public Map<String, Object> openIdConfiguration(UriComponentsBuilder builder) {
          return Map.of("issuer",issuer,
                "authorization_endpoint", authorization_endpoint,
                "token_endpoint",  issuer,
                "jwks_uri", token_keys,
                "subject_types_supported", List.of("public"));
    }

    /**
     * https://docs.spring.io/spring-security-oauth2-boot/docs/2.3.x-SNAPSHOT/reference/html5/#oauth2-boot-authorization-server-spring-security-oauth2-resource-server-jwk-set-uri
     */
    @GetMapping(path = "token_keys")
    public Map<String, Object> tokenKeys() {
        final RSAPublicKey publicKey = (RSAPublicKey) this.keyPair.getPublic();
        final RSAKey key = new RSAKey.Builder(publicKey).build();
        return new JWKSet(key).toJSONObject();
    }
}

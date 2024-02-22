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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

@ConfigurationProperties(prefix = "jwt", ignoreUnknownFields = false)
@Component
@Primary
public class JwtProperties {

    private String privatekey;

    private String publickey;

    public String getPrivatekey() {
        return privatekey;
    }

    public void setPrivatekey(String privatekey) {
        this.privatekey = privatekey;
    }

    public String getPublickey() {
        return publickey;
    }

    public void setPublickey(String publickey) {
        this.publickey = publickey;
    }


    private KeyPair keyPair;

    public KeyPair getKeyPair(String privateKey, String publicKey) {
        try {
            this.keyPair = new KeyPair(resourceToPublicKey(publicKey), resourceToPrivateKey(privateKey));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return keyPair;
    }

    private static PublicKey resourceToPublicKey(String pk) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final String key = pk
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .trim()
                .replace("\r\n", "")
                .replace("\n", "");
        final byte[] decode = Base64Utils.decodeFromString(key);
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpec);
        return publicKey;
    }

    private static PrivateKey resourceToPrivateKey(String prK) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final byte[] decoded = Base64Utils.decodeFromString(prK
                .replace("fake", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .trim()
                .replace("\r\n", "")
                .replace("\n", ""));
        final EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }


    private static String resourceToString(Resource resource) {
        try (InputStream stream = resource.getInputStream()) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

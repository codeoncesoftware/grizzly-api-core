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
package fr.codeonce.grizzly.core.service.util;

import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;

public class IdentityProviderUtils {

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    public IdentityProviderUtils() {
    }

    public JSONObject parser(String value) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(value);
    }

    public StringBuilder actualIdentityProviderID(Project project, String idp) {
        StringBuilder idpID = new StringBuilder();
        project.getIdentityProviderIds().stream().forEach(ipID -> {
            if (identityProviderRepository.findById(ipID).get().getName().toString().equals(idp.toUpperCase())) {
                idpID.append(ipID);
            }
        });
        return idpID;
    }
}

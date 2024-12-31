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
package fr.codeonce.grizzly.core.service.swagger.utils;

import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DefinitonGeneratorOpenApi {
    private static final Logger log = LoggerFactory.getLogger(DefinitonGeneratorOpenApi.class);

    public Model getSignUp() {
        Model user = new ModelImpl();
        Map<String, Property> properties = new LinkedHashMap<>();
        properties.put("firstname", PropertyBuilder.build("string", null, null));
        properties.put("lastname", PropertyBuilder.build("string", null, null));
        properties.put("username", PropertyBuilder.build("string", null, null));
        properties.put("password", PropertyBuilder.build("string", null, null));
        properties.put("email", PropertyBuilder.build("string", null, null));
        properties.put("phone", PropertyBuilder.build("number", null, null));
        user.setProperties(properties);
        return user;
    }

    public Model getSignIn() {
        Model user = new ModelImpl();
        Map<String, Property> properties = new LinkedHashMap<>();
        properties.put("username", PropertyBuilder.build("string", null, null));
        properties.put("password", PropertyBuilder.build("string", null, null));
        user.setProperties(properties);
        return user;
    }


    private Map<String, Property> fetchEndpointModel(EndpointModel model) {
        Map<String, Property> endpointProperties = new LinkedHashMap<String, Property>();
        model.getProperties().forEach(prop -> {
            if (prop.getRef() != null && !prop.getRef().equals("")) {
                if (prop.getArray() == true) {
                    RefProperty mo = new RefProperty("#/definitions/" + prop.getRef());
                    ArrayProperty arrayProperty = new ArrayProperty(mo);
                    arrayProperty.setExample(prop.getExample());
                    arrayProperty.setDescription(prop.getDescription());
                    endpointProperties.put(prop.getName(), (Property) arrayProperty);
                } else {
                    RefProperty mo = new RefProperty("#/definitions/" + prop.getRef());
                    mo.setExample(prop.getExample());
                    mo.setDescription(prop.getDescription());
                    endpointProperties.put(prop.getName(), (Property) mo);
                }
            } else {
                if (prop.getArray() != null) {
                    if (prop.getArray() == true) {
                        ArrayProperty arrayProperty = new ArrayProperty(
                                PropertyBuilder.build(prop.getType(), null, null));
                        arrayProperty.setExample(prop.getExample());
                        arrayProperty.setDescription(prop.getDescription());
                        endpointProperties.put(prop.getName(), (Property) arrayProperty);
                    } else {
                        Property p = PropertyBuilder.build(prop.getType(), null, null);
                        if (prop.getExample() != null) {
                            p.setExample(prop.getExample());
                        }

                        p.setDescription(prop.getDescription());
                        endpointProperties.put(prop.getName(), p);
                    }
                } else {
                    Property p = PropertyBuilder.build(prop.getType(), null, null);
                    p.setExample(prop.getExample());
                    p.setDescription(prop.getDescription());
                    endpointProperties.put(prop.getName(), p);
                }

            }


        });
        return endpointProperties;
    }
}

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;

import java.io.IOException;

/**
 * io.swagger.models.Model is an Interface, so the Object Mapper Can't determine with Class to use for Instantiation
 * This Deserializer is configured to use ModelImpl to return a valid Model Object
 *
 * @author rayen
 */
public class ModelDeserializer extends StdDeserializer<io.swagger.models.Model> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ModelDeserializer() {
        this(null);
    }

    public ModelDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Model deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ModelImpl model = new ModelImpl();
        Property property = PropertyBuilder.build("string", null, null);
        model.setType("object");
        model.addProperty("text", property);
        return model;

    }
}

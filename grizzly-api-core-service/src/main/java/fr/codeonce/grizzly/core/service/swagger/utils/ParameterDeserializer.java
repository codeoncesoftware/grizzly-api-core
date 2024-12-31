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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.models.parameters.*;

import java.io.IOException;

/**
 * io.swagger.models.parameters.Parameter is an Interface, so the Object Mapper Can't determine with Class to use for Instantiation
 * This Deserializer is configured to use the Concrete classes to return a valid Parameter Object
 * Classes used : PathParameter, BodyParameter, QueryParameter, HeaderParameter, FormParameter
 *
 * @author rayen
 */
public class ParameterDeserializer extends StdDeserializer<io.swagger.models.parameters.Parameter> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ParameterDeserializer() {
        this(null);
    }

    public ParameterDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Parameter deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        final JsonNode node = parser.getCodec().readTree(parser);
        final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        if (node.get("in").toString().toLowerCase().replaceAll("\"", "").equals("path")) {
            return mapper.treeToValue(node, PathParameter.class);
        } else if (node.get("in").toString().toLowerCase().replaceAll("\"", "").equals("body")) {
            return mapper.treeToValue(node, BodyParameter.class);
        } else if (node.get("in").toString().toLowerCase().replaceAll("\"", "").equals("query")) {
            return mapper.treeToValue(node, QueryParameter.class);
        } else if (node.get("in").toString().toLowerCase().replaceAll("\"", "").equals("header")) {
            return mapper.treeToValue(node, HeaderParameter.class);
        } else {
            return mapper.treeToValue(node, FormParameter.class);
        }
    }

}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.core.domain.resource.ExampleObject;
import fr.codeonce.grizzly.core.domain.resource.Items;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ParamOpenApiFactory {
    private static final Logger log = LoggerFactory.getLogger(ParamOpenApiFactory.class);

    public ParamOpenApiFactory() {
    }

    private static class OpenApiParameterFactoryHolder {
        static final ParamOpenApiFactory FACTORY = new ParamOpenApiFactory();
    }

    public static ParamOpenApiFactory getInstance() {
        return OpenApiParameterFactoryHolder.FACTORY;
    }

    public io.swagger.v3.oas.models.parameters.RequestBody getModelDef(String type, String description) {
        Content content = new Content();
        io.swagger.v3.oas.models.parameters.RequestBody rb = new io.swagger.v3.oas.models.parameters.RequestBody();
        MediaType mediaType = new MediaType();
        Schema schema = new Schema();
        if (type.equalsIgnoreCase("signin")) {
            schema.set$ref("signIn");
        } else if (type.equalsIgnoreCase("signup")) {
            schema.set$ref("signUp");
        }
        schema.setDescription(description);
        mediaType.setSchema(schema);
        content.put("application/json", mediaType);
        rb.setContent(content);

        return rb;
    }

    public Parameter makeParameter(ResourceParameter param) {
        if (!param.getIn().equalsIgnoreCase("body")) {
            Parameter parameter = new Parameter();
            parameter.setName(param.getName());
            parameter.setRequired(param.getRequired());
            if (param.getIn() != null) {
                parameter.setIn(param.getIn());
            }
            if (param.getDescription() != null) {
                parameter.setDescription(param.getDescription());
            }
            if (param.getType() != null) {
                if (param.getType().equals("array")) {
                    ArraySchema schema = new ArraySchema();
                    Schema items = new Schema();
                    schema.setType(param.getType());
                    schema.setFormat(param.getFormat());
                    schema.setMaximum(param.getMaximum());
                    schema.setMinimum(param.getMinimum());
                    schema.setPattern(param.getPattern());
                    schema.setDefault(param.getDefaultValue());

                    items.setType(param.getItems().getType());
                    if (param.getModelName() != null) {
                        items.$ref("#/components/schemas/" + param.getModelName());
                    }

                    schema.setItems(items);
                    parameter.setSchema(schema);
                } else {
                    Schema schema = new Schema();
                    schema.setType(param.getType());
                    schema.setFormat(param.getFormat());
                    schema.setMaximum(param.getMaximum());
                    schema.setMinimum(param.getMinimum());
                    schema.setPattern(param.getPattern());
                    schema.setDefault(param.getDefaultValue());
                    if (param.getModelName() != null) {
                        schema.set$ref("#/components/schemas/" + param.getModelName());
                    }
                    parameter.setSchema(schema);
                }
            }

            Map<String, Example> examples = new HashMap<String, Example>();
            if (param.getExamples() != null) {
                param.getExamples().forEach((k, v) -> {
                    Example example = new Example();
                    example.set$ref(v.get$ref());
                    example.setDescription(v.getDescription());
                    example.setExternalValue(v.getExternalValue());
                    example.setSummary(v.getSummary());
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        example.setValue(convertToObject(mapper.readTree(v.getValue())));
                    } catch (JsonProcessingException e) {
                        log.error("error in json parsing", e);
                    }
                    examples.put(k, example);
                });
                parameter.setExamples(examples);
            }
            return parameter;
        }
        return null;
    }

    public ResourceParameter makeResourceParameter(Parameter param) {

        ResourceParameter resourceParameter = new ResourceParameter();
        resourceParameter.setIn(param.getIn());
        resourceParameter.setDescription(param.getDescription());
        resourceParameter.setName(param.getName());
        if (param.getSchema() != null) {
            resourceParameter.setType(param.getSchema().getType());
            if (param.getSchema().get$ref() != null) {
                resourceParameter.setModelName(param.getSchema().get$ref().substring("#/components/schemas/".length(), param.getSchema().get$ref().length()));
                resourceParameter.setType("object");
            }
            if (param.getSchema().getType() == "array") {
                ArraySchema arrSch = (ArraySchema) param.getSchema();
                Items items = new Items();
                items.setType(arrSch.getItems().getType());
                resourceParameter.setItems(items);
            }
            resourceParameter.setFormat(param.getSchema().getFormat());
            resourceParameter.setRequired(param.getRequired());
            resourceParameter.setEnums(param.getSchema().getEnum());
            resourceParameter.setMaximum(param.getSchema().getMaximum());
            resourceParameter.setMinimum(param.getSchema().getMinimum());
            resourceParameter.setPattern(param.getSchema().getPattern());
            if (param.getSchema().getDefault() != null)
                resourceParameter.setDefaultValue(param.getSchema().getDefault().toString());
        }

        if (param.getExamples() != null) {
            Map<String, ExampleObject> examples = new HashMap<String, ExampleObject>();
            param.getExamples().forEach((k, v) -> {
                ExampleObject example = new ExampleObject();
                example.set$ref(v.get$ref());
                example.setDescription(v.getDescription());
                example.setExternalValue(v.getExternalValue());
                example.setSummary(v.getSummary());
                ObjectMapper mapper = new ObjectMapper();
                try {
                    example.setValue(mapper.writeValueAsString(v.getValue()));
                } catch (JsonProcessingException e) {
                    log.error("error in json parsing", e);
                }
                examples.put(k, example);
            });
            resourceParameter.setExamples(examples);
        }

        if (param.get$ref() != null) {
            resourceParameter.setModelName(
                    param.get$ref().substring("#/components/requestBodies/".length(), param.get$ref().length()));
        }

        return resourceParameter;
    }

    public String safeStringReturn(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Object convertToObject(JsonNode value) {
        return value.deepCopy();

    }
}

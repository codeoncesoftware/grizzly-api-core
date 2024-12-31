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

import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import fr.codeonce.grizzly.core.domain.endpointModel.ModelProperty;
import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefinitonGenerator implements IDefinitionGenerator {

    private static final String schemas = "#/components/schemas/";

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

    public Model getUser() {
        Model user = new ModelImpl();
        Map<String, Property> properties = new LinkedHashMap<>();
        properties.put("_id", PropertyBuilder.build("string", null, null));
        properties.put("password", PropertyBuilder.build("string", null, null));
        properties.put("firstname", PropertyBuilder.build("string", null, null));
        properties.put("phone", PropertyBuilder.build("string", null, null));
        ArrayProperty roles = new ArrayProperty();
        roles.setType("string");
        properties.put("roles", roles);
        properties.put("email", PropertyBuilder.build("string", null, null));
        properties.put("enabled", PropertyBuilder.build("boolean", null, null));
        properties.put("username", PropertyBuilder.build("string", null, null));
        properties.put("lastname", PropertyBuilder.build("string", null, null));
        user.setProperties(properties);
        return user;
    }

    @Override
    public Model getPet() {
        Model pet = new ModelImpl();
        Map<String, Property> properties = new LinkedHashMap<>();
        properties.put("name", PropertyBuilder.build("string", null, null));
        properties.put("type", PropertyBuilder.build("string", null, null));
        properties.put("price", PropertyBuilder.build("number", null, null));
        properties.put("quantity", PropertyBuilder.build("number", null, null));
        properties.put("status", PropertyBuilder.build("string", null, null));
        properties.put("photo", PropertyBuilder.build("string", null, null));
        pet.setProperties(properties);
        return pet;
    }

    @SuppressWarnings("deprecation")
    public List<Model> modelDefintion(Container container) {

        List<Model> models = new ArrayList<Model>();
        container.getEndpointModels().forEach(model -> {
            if (model.getArray() != null) {
                if (model.getArray() == true) {
                    ArrayModel arrModel = new ArrayModel();
                    arrModel.setTitle(model.getTitle());
                    arrModel.setType("array");
                    arrModel.setExample(model.getExample());
                    if (model.getDescription() != null) {
                        arrModel.setDescription(model.getDescription().trim());
                    }

                    if (model.getRef() != null) {
                        RefProperty refProperty = new RefProperty();
                        refProperty.set$ref("#/definitions/" + model.getRef());
                        arrModel.setItems(refProperty);
                        models.add(arrModel);
                    } else {
                        if (model.getType().equals("boolean")) {
                            Property p = PropertyBuilder.build("boolean", null, null);
                            arrModel.setItems(p);
                            models.add(arrModel);
                        } else if (model.getType().equals("string")) {
                            Property p = PropertyBuilder.build("string", null, null);
                            arrModel.setItems(p);
                            models.add(arrModel);
                        }
                    }

                }
            } else {
                if (model.getAllOf() != null && !model.getAllOf().isEmpty()) {
                    ComposedModel endpointModel = new ComposedModel();
                    List<Model> mds = new ArrayList<Model>();
                    model.getAllOf().forEach(ref -> {
                        RefModel rm = new RefModel();
                        rm.set$ref(ref);
                        Map<String, Property> endpointProperties = new LinkedHashMap<String, Property>();
                        endpointProperties = fetchEndpointModel(model);
                        rm.setProperties(endpointProperties);
                        mds.add(rm);
                    });
                    Map<String, Property> endpointProperties = new LinkedHashMap<String, Property>();
                    endpointProperties = fetchEndpointModel(model);
                    endpointModel.setProperties(endpointProperties);
                    endpointModel.setAllOf(mds);
                    endpointModel.setTitle(model.getTitle());
                    endpointModel.setDescription(model.getDescription());
                    models.add(endpointModel);

                } else {
                    ModelImpl endpointModel = new ModelImpl();
                    Map<String, Property> endpointProperties = new LinkedHashMap<>();
                    if (model.getProperties() != null) {
                        endpointProperties = fetchEndpointModel(model);
                    }
                    endpointModel.setFormat(model.getFormat());
                    endpointModel.setExample(model.getExample());
                    endpointModel.setRequired(model.getRequired());
                    if (model.getEnums() != null && !model.getEnums().isEmpty()) {
                        List<String> enums = new ArrayList<>();
                        model.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        endpointModel.setEnum(enums);
                    }
                    endpointModel.setDescription(model.getDescription());
                    endpointModel.setType(model.getType());
                    endpointModel.setProperties(endpointProperties);
                    endpointModel.setTitle(model.getTitle());
                    models.add(endpointModel);
                }

            }

        });
        return models;
    }

    private Map<String, Property> fetchEndpointModel(EndpointModel model) {
        Map<String, Property> endpointProperties = new LinkedHashMap<String, Property>();
        model.getProperties().forEach(prop -> {
            if (prop.getRef() != null && !prop.getRef().equals("")) {
                if (prop.getArray() == true) {
                    RefProperty mo = new RefProperty("#/definitions/" + prop.getRef());
                    ArrayProperty arrayProperty = new ArrayProperty(mo);
                    arrayProperty.setExample(prop.getExample());
                    if (prop.getDescription() != null) {
                        arrayProperty.setDescription(prop.getDescription().trim());
                    }

                    endpointProperties.put(prop.getName(), (Property) arrayProperty);
                } else {
                    RefProperty mo = new RefProperty("#/definitions/" + prop.getRef());
                    mo.setExample(prop.getExample());
                    if (prop.getDescription() != null) {
                        mo.setDescription(prop.getDescription().trim());
                    }

                    endpointProperties.put(prop.getName(), (Property) mo);
                }
            } else {
                if (prop.getArray() != null) {
                    if (prop.getArray() == true) {
                        ArrayProperty arrayProperty = new ArrayProperty(
                                PropertyBuilder.build(prop.getType(), null, null));
                        arrayProperty.setExample(prop.getExample());
                        if (prop.getDescription() != null) {
                            arrayProperty.setDescription(prop.getDescription().trim());
                        }

                        endpointProperties.put(prop.getName(), (Property) arrayProperty);
                    } else {
                        String prope = "";
                        String format = null;
                        if (prop.getType().equalsIgnoreCase("date")) {
                            prope = "string";
                            format = "date-time";
                        } else {
                            prope = prop.getType();
                        }
                        Property p = PropertyBuilder.build(prope, format, null);
                        if (prop.getExample() != null) {
                            p.setExample(prop.getExample());
                        }
                        if (prop.getDescription() != null) {
                            p.setDescription(prop.getDescription().trim());
                        }

                        endpointProperties.put(prop.getName(), p);
                    }
                } else {
                    Property p = PropertyBuilder.build(prop.getType(), null, null);
                    p.setExample(prop.getExample());
                    if (prop.getDescription() != null) {
                        p.setDescription(prop.getDescription().trim());
                    }
                    endpointProperties.put(prop.getName(), p);
                }

            }
        });
        return endpointProperties;
    }

    // open api generator

    public Map<String, Schema> generateSchemaDefiniton(Container container) {
        Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();
        if (container.getEndpointModels() != null) {
            container.getEndpointModels().forEach(model -> {
                fetchSchema(model, schemas);
            });
        }

        return schemas;
    }

    private void fetchSchema(EndpointModel model, Map<String, Schema> schemas) {
        if (model.getArray() != null) {
            if (model.getArray() == true) {
                ArraySchema arraySchema = new ArraySchema();
                arraySchema.setTitle(model.getTitle());
                arraySchema.setType("array");
                arraySchema.setExample(model.getExample());
                if (model.getDescription() != null) {
                    arraySchema.setDescription(model.getDescription().trim());
                }

                Schema prop = new Schema();


                if (model.getRef() != null) {
                    prop.set$ref(DefinitonGenerator.schemas + model.getRef());
                    arraySchema.setItems(prop);
                    schemas.put(model.getTitle(), arraySchema);

                } else {
                    if (model.getType().equals("boolean")) {
                        prop.setType("boolean");
                        arraySchema.setItems(prop);
                        schemas.put(model.getTitle(), arraySchema);
                    } else if (model.getType().equals("string")) {
                        prop.setType("string");
                        arraySchema.setItems(prop);
                        schemas.put(model.getTitle(), arraySchema);
                    }
                }

            }
        } else {
            Schema schema = new Schema();
            schema.setRequired(model.getRequired());
            if (model.getDescription() != null) {
                schema.setDescription(model.getDescription().trim());
            }
            if (model.getEnums() != null && !model.getEnums().isEmpty()) {
                List<String> enums = new ArrayList<>();
                model.getEnums().forEach(e -> {
                    enums.add(e.trim());
                });
                schema.setEnum(enums);
            }

            schema.setType(model.getType());
            schema.setTitle(model.getTitle());
            Map<String, Schema> embeddedProperties = new LinkedHashMap<String, Schema>();
            if (model.getProperties() != null) {
                model.getProperties().forEach(prop -> {
                    fetchProperty(prop, schema, embeddedProperties);
                });
            }

            schemas.put(model.getTitle(), schema);
        }

    }

    private Map<String, Schema> fetchProperty(ModelProperty model, Schema schema, Map<String, Schema> embeddedProperties) {
        if (model.getRef() != null && !model.getRef().equals("")) {
            if (model.getArray()) {
                ArraySchema embeddedSchema = new ArraySchema();
                embeddedSchema.setType("array");
                Schema items = new Schema();
                items.set$ref(DefinitonGenerator.schemas + model.getRef());
                embeddedSchema.setItems(items);
                embeddedSchema.setExample(model.getExample());
                if (model.getDescription() != null) {
                    embeddedSchema.setDescription(model.getDescription().trim());
                }

                embeddedProperties.put(model.getName(), embeddedSchema);
            } else {
                Schema embeddedSchema = new Schema();
                if (model.getDescription() != null) {
                    embeddedSchema.setDescription(model.getDescription().trim());
                }
                embeddedSchema.setType(model.getType());
                if (model.getEnums() != null) {
                    if (model.getEnums().size() != 0) {
                        List<String> enums = new ArrayList<String>();
                        model.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        embeddedSchema.setEnum(enums);
                    }
                }

                embeddedSchema.set$ref(DefinitonGenerator.schemas + model.getRef());
                if (model.getExample() != null) {
                    embeddedSchema.setExample(model.getExample().trim());
                }
                embeddedProperties.put(model.getName(), embeddedSchema);
            }
        } else {
            if (model.getArray() != null) {
                if (model.getArray()) {
                    ArraySchema embeddedSchema = new ArraySchema();
                    Schema items = new Schema();
                    items.setType(model.getType());
                    embeddedSchema.setItems(items);
                    embeddedSchema.setType("array");
                    if (model.getExample() != null) {
                        embeddedSchema.setExample(model.getExample().trim());
                    }
                    if (model.getDescription() != null) {
                        embeddedSchema.setDescription(model.getDescription().trim());
                    }
                    embeddedProperties.put(model.getName(), embeddedSchema);
                } else {
                    Schema embeddedSchema = new Schema();
                    if (model.getEnums() != null && !model.getEnums().isEmpty()) {
                        List<String> enums = new ArrayList<String>();
                        model.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        embeddedSchema.setEnum(enums);
                    }

                    if (model.getDescription() != null) {
                        embeddedSchema.setDescription(model.getDescription().trim());
                    }
                    embeddedSchema.setType(model.getType());
                    if (model.getExample() != null) {
                        embeddedSchema.setExample(model.getExample().trim());
                    }
                    embeddedProperties.put(model.getName(), embeddedSchema);
                }
            } else {
                Schema embeddedSchema = new Schema();
                if (model.getEnums().size() != 0) {
                    embeddedSchema.setEnum(model.getEnums());
                }

                if (model.getDescription() != null) {
                    embeddedSchema.setDescription(model.getDescription().trim());
                }
                embeddedSchema.setType(model.getType());
                if (model.getExample() != null) {
                    embeddedSchema.setExample(model.getExample().trim());
                }

                embeddedProperties.put(model.getName(), embeddedSchema);
            }
        }
        schema.setProperties(embeddedProperties);

        return embeddedProperties;
    }
}

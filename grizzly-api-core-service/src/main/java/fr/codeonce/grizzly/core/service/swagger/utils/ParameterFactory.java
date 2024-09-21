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

import fr.codeonce.grizzly.core.domain.resource.Items;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;
import io.swagger.models.ArrayModel;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * This Parameter Factory is a singleton Responsible for Parameter and
 * ResourceParameter instantiation
 *
 * @author rayen
 */
public class ParameterFactory {


    public ParameterFactory() {
    }

    /**
     * Holder Class
     *
     * @author rayen
     */
    private static class ParameterFactoryHolder {
        static final ParameterFactory FACTORY = new ParameterFactory();
    }

    public static ParameterFactory getInstance() {
        return ParameterFactoryHolder.FACTORY;
    }

    /**
     * Return a Parameter Object and set it's Fields from the given
     * ResourceParameter object
     *
     * @param param
     * @return
     */
    public Parameter makeParameter(ResourceParameter param) {
        if (param != null && param.getIn() != null) {
            switch (param.getIn().toLowerCase()) {
                case "body":
                    if (param.getModelName() != null) {
                        BodyParameter bodyParameter = new BodyParameter();
                        bodyParameter.setName(param.getName());
                        if (param.getType() != null) {
                            if (param.getType().equals("array")) {
                                RefProperty mo = new RefProperty("#/definitions/" + param.getModelName());
                                ArrayModel arrModel = new ArrayModel();
                                arrModel.setItems(mo);
                                bodyParameter.setSchema(arrModel);

                            } else {
                                RefModel schema = new RefModel();
                                schema.set$ref(param.getModelName());
                                bodyParameter.setSchema(schema);
                            }
                        }

                        if (param.getDescription() != null) {
                            bodyParameter.setDescription(param.getDescription());
                        }
                        if (param.getRequired() != null) {
                            bodyParameter.setRequired(param.getRequired());
                        }

                        return bodyParameter;
                    } else {
                        BodyParameter bodyParameter = new BodyParameter();
                        bodyParameter.setName(param.getName());
                        if (!param.getType().equals("string")) {
                            ModelImpl model = new ModelImpl();
                            model.setType("object");
                            bodyParameter.setSchema(model);
                        } else {
                            ModelImpl model = new ModelImpl();
                            bodyParameter.setSchema(model);
                        }

                        if (param.getDescription() != null) {
                            bodyParameter.setDescription(param.getDescription());
                        }
                        if (param.getRequired() != null) {
                            bodyParameter.setRequired(param.getRequired());
                        }

                        return bodyParameter;
                    }

                case "header":
                    HeaderParameter headerParam = new HeaderParameter();
                    headerParam.setName(param.getName());
                    if (param.getType() != null) {
                        headerParam.setType(param.getType().toLowerCase());
                    }
                    headerParam.setDefaultValue(param.getValue());
                    if (param.getDescription() != null) {
                        headerParam.setDescription(param.getDescription());
                    }
                    if (param.getRequired() != null) {
                        headerParam.setRequired(param.getRequired());
                    }
                    if (param.getFormat() != null) {
                        headerParam.setFormat(param.getFormat());
                    }
                    if (param.getDefaultValue() != null) {
                        headerParam.setDefaultValue(param.getDefaultValue());
                    }
                    if (param.getMaximum() != null) {
                        headerParam.setMaximum(param.getMaximum());
                    }
                    if (param.getMinimum() != null) {
                        headerParam.setMinimum(param.getMinimum());
                    }
                    if (param.getEnums() != null && param.getEnums().size() != 0) {
                        List<String> enums = new ArrayList<String>();
                        param.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        headerParam.setEnum(enums);
                    }
                    if (param.getItems() != null) {
                        if (param.getItems().getType().equals("string")) {
                            StringProperty pr = setDefaults(param);
                            headerParam.setItems(pr);
                        } else {

                            headerParam.setItems(PropertyBuilder.build(param.getItems().getType(), null, null));
                        }

                    }
                    return headerParam;
                case "formdata":
                    FormParameter formParam = new FormParameter();
                    formParam.setName(param.getName());
                    if (param.getType() != null) {
                        formParam.setType(param.getType().toLowerCase());
                    }

                    formParam.setDefaultValue(param.getValue());
                    if (param.getDescription() != null) {
                        formParam.setDescription(param.getDescription());
                    }
                    if (param.getRequired() != null) {
                        formParam.setRequired(param.getRequired());
                    }
                    if (param.getFormat() != null) {
                        formParam.setFormat(param.getFormat());
                    }
                    if (param.getMaximum() != null) {
                        formParam.setMaximum(param.getMaximum());
                    }
                    if (param.getMinimum() != null) {
                        formParam.setMinimum(param.getMinimum());
                    }
                    if (param.getEnums() != null && param.getEnums().size() != 0) {
                        List<String> enums = new ArrayList<>();
                        param.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        formParam.setEnum(enums);
                    }
                    if (param.getDefaultValue() != null) {
                        formParam.setDefaultValue(param.getDefaultValue());
                    }

                    if (param.getItems() != null) {

                        if (param.getItems().getType().equals("string")) {
                            StringProperty pr = setDefaults(param);
                            formParam.setItems(pr);
                        } else {

                            formParam.setItems(PropertyBuilder.build(param.getItems().getType(), null, null));
                        }

                    }

                    return formParam;
                case "query":
                    QueryParameter queryParam = new QueryParameter();
                    queryParam.setName(param.getName());
                    if (param.getType() != null) {
                        queryParam.setType(param.getType().toLowerCase());
                    }
                    queryParam.setDefaultValue(param.getValue());
                    if (param.getDescription() != null) {
                        queryParam.setDescription(param.getDescription());
                    }
                    if (param.getRequired() != null) {
                        queryParam.setRequired(param.getRequired());
                    }
                    if (param.getFormat() != null) {
                        queryParam.setFormat(param.getFormat());
                    }
                    if (param.getMaximum() != null) {
                        queryParam.setMaximum(param.getMaximum());
                    }
                    if (param.getMinimum() != null) {
                        queryParam.setMinimum(param.getMinimum());
                    }
                    if (param.getEnums() != null && param.getEnums().size() != 0) {
                        List<String> enums = new ArrayList<String>();
                        param.getEnums().forEach(e -> {
                            enums.add(e.trim());
                        });
                        queryParam.setEnum(enums);
                    }
                    if (param.getDefaultValue() != null) {
                        queryParam.setDefaultValue(param.getDefaultValue());
                    }
                    if (param.getItems() != null) {
                        if (param.getItems().getType().equals("string")) {
                            StringProperty pr = new StringProperty();
                            pr.setType(param.getItems().getType());
                            pr.setEnum(param.getItems().getEnums());
                            pr.setDefault(param.getItems().getDefaultValue());
                            queryParam.setItems(pr);
                        } else {
                            queryParam.setItems(PropertyBuilder.build(param.getItems().getType(), null, null));
                        }

                    }

                    return queryParam;
                // Default is Path Parameter
                default:
                    PathParameter pathParam = new PathParameter();
                    pathParam.setName(param.getName());
                    if (param.getType() != null) {
                        pathParam.setType(param.getType().toLowerCase());
                    }

                    pathParam.setDefaultValue(param.getValue());
                    if (param.getDescription() != null) {
                        pathParam.setDescription(param.getDescription());
                    }
                    if (param.getRequired() != null) {
                        pathParam.setRequired(param.getRequired());
                    }
                    if (param.getFormat() != null) {
                        pathParam.setFormat(param.getFormat());
                    }
                    if (param.getMaximum() != null) {
                        pathParam.setMaximum(param.getMaximum());
                    }
                    if (param.getMinimum() != null) {
                        pathParam.setMinimum(param.getMinimum());
                    }
                    if (param.getEnums() != null) {
                        pathParam.setEnum(param.getEnums());
                    }
                    if (param.getDefaultValue() != null) {
                        pathParam.setDefaultValue(param.getDefaultValue());
                    }
                    if (param.getItems() != null) {
                        if (param.getItems().getType().equals("string")) {
                            StringProperty pr = setDefaults(param);
                            pathParam.setItems(pr);
                        } else {

                            pathParam.setItems(PropertyBuilder.build(param.getItems().getType(), null, null));
                        }

                    }
                    return pathParam;
            }

        }

        return null;
    }

    /**
     * Prepare Models Definitions for Sign-Up and Sign-In
     *
     * @param type
     * @return Parameter of type RefModel
     */
    public Parameter getModelDef(String type) {

        BodyParameter bodyParameter = new BodyParameter();
        bodyParameter.setName("body");
        bodyParameter.setRequired(true);

        if (type.equals("signup")) {
            bodyParameter.setDescription("SignUp Model");
            bodyParameter.setSchema(new RefModel("signUp"));
        } else if (type.equals("signin")) {
            bodyParameter.setDescription("SignIn Model");
            bodyParameter.setSchema(new RefModel("signIn"));
        } else if (type.equals("pet")) {
            bodyParameter.setDescription("Pet Model");
            bodyParameter.setSchema(new RefModel("Pet"));
        } else if (type.equals("roles")) {
            bodyParameter.setDescription("roles model");
            bodyParameter.setSchema(new RefModel("roles"));
        } else if (type.equals("authUser")) {
            bodyParameter.setDescription("User model");
            bodyParameter.setSchema(new RefModel("authUser"));

        }

        return bodyParameter;
    }

    /**
     * Return a valid ResourceParameter object and set it's fields from the given
     * Parameter Object
     *
     * @param param
     * @return
     */
    public ResourceParameter makeResourceParameter(Parameter param) {
        if (param instanceof BodyParameter bodyParam) {
            String desc = null;
            ResourceParameter resourceParameter = new ResourceParameter("body", "object", "", "Body");
            if (bodyParam.getSchema().getClass().equals(RefModel.class)) {
                resourceParameter.setModelName(bodyParam.getSchema().getReference().substring("#/definitions/".length(),
                        bodyParam.getSchema().getReference().length()));
                resourceParameter.setDescription(bodyParam.getDescription());
                resourceParameter.setRequired(bodyParam.getRequired());
                return resourceParameter;
            } else if (bodyParam.getSchema().getClass().equals(ArrayModel.class)) {
                ArrayModel arrayModel = (ArrayModel) bodyParam.getSchema();
                if (!arrayModel.getItems().getClass().equals(StringProperty.class)) {
                    RefProperty refProp = (RefProperty) arrayModel.getItems();
                    resourceParameter.setModelName(
                            refProp.get$ref().substring("#/definitions/".length(), refProp.get$ref().length()));
                } else {
                    Items items = new Items();
                    items.setType("string");
                    resourceParameter.setItems(items);
                }

                resourceParameter.setDescription(bodyParam.getDescription());
                resourceParameter.setType("array");
                resourceParameter.setRequired(bodyParam.getRequired());

                return resourceParameter;
            }

            if (bodyParam.getSchema() != null && bodyParam.getSchema().getProperties() != null
                    && bodyParam.getSchema().getProperties().get("text") != null) {
                desc = bodyParam.getSchema().getProperties().get("text").getDescription();
            }
            ResourceParameter rp = new ResourceParameter(safeStringReturn(safeStringReturn(bodyParam.getName())),
                    "String", desc, bodyParam.getIn());
            rp.setDescription(bodyParam.getDescription());
            rp.setRequired(bodyParam.getRequired());

            return rp;
        } else if (param instanceof HeaderParameter headerParam) {
            ResourceParameter rp = new ResourceParameter(safeStringReturn(headerParam.getName()), headerParam.getType(),
                    safeStringReturn(headerParam.getDefaultValue()), headerParam.getIn());
            if (headerParam.getType() != null && headerParam.getType().equals("array")) {
                Items items = new Items();
                items.setType(headerParam.getItems().getType());
                if (headerParam.getDefault() != null) {
                    items.setDefaultValue(headerParam.getDefault().toString());
                }

                rp.setItems(items);
            }

            rp.setDescription(headerParam.getDescription());
            rp.setFormat(headerParam.getFormat());
            rp.setMaximum(headerParam.getMaximum());
            rp.setMinimum(headerParam.getMinimum());
            rp.setRequired(headerParam.getRequired());
            rp.setEnums(headerParam.getEnum());
            if (headerParam.getDefaultValue() != null) {
                rp.setDefaultValue(headerParam.getDefaultValue().toString());
            }

            return rp;
        } else if (param instanceof FormParameter formParam) {
            ResourceParameter rp = new ResourceParameter(safeStringReturn(formParam.getName()), formParam.getType(),
                    safeStringReturn(formParam.getDefaultValue()), formParam.getIn());
            rp.setDescription(formParam.getDescription());
            if (formParam.getType() != null) {
                if (formParam.getType().equals("array")) {
                    Items items = new Items();
                    items.setType(formParam.getItems().getType());
                    if (formParam.getDefault() != null) {
                        items.setDefaultValue(formParam.getDefault().toString());
                    }
                    rp.setItems(items);
                }
            }
            if (formParam.getDefaultValue() != null) {
                rp.setDefaultValue(formParam.getDefaultValue().toString());
            }

            rp.setRequired(formParam.getRequired());
            rp.setFormat(formParam.getFormat());
            rp.setMaximum(formParam.getMaximum());
            rp.setMinimum(formParam.getMinimum());
            rp.setEnums(formParam.getEnum());
            return rp;
        } else if (param instanceof QueryParameter queryParam) {
            ResourceParameter rp = new ResourceParameter(safeStringReturn(queryParam.getName()), queryParam.getType(),
                    safeStringReturn(queryParam.getDefaultValue()), queryParam.getIn());
            if (queryParam.getType() != null && queryParam.getType().equals("array")) {
                Items items = new Items();
                items.setType(queryParam.getItems().getType());
                if (queryParam.getItems().getClass().equals(StringProperty.class)) {
                    StringProperty sp = (StringProperty) queryParam.getItems();
                    items.setEnums(sp.getEnum());
                    items.setDefaultValue(sp.getDefault());
                }

                rp.setItems(items);
            }
            if (queryParam.getDefaultValue() != null) {
                rp.setDefaultValue(queryParam.getDefaultValue().toString());
            }

            rp.setDescription(queryParam.getDescription());
            rp.setRequired(queryParam.getRequired());
            rp.setFormat(queryParam.getFormat());
            rp.setMaximum(queryParam.getMaximum());
            rp.setMinimum(queryParam.getMinimum());
            rp.setEnums(queryParam.getEnum());
            return rp;
        } else if (param instanceof RefParameter refParam) {
            ResourceParameter rp = new ResourceParameter(safeStringReturn(refParam.getName()), null, null,
                    refParam.getIn());
            rp.setRequired(refParam.getRequired());
            return rp;

        } else {

            PathParameter pathParam = (PathParameter) param;
            ResourceParameter rp = new ResourceParameter(safeStringReturn(pathParam.getName()), pathParam.getType(),
                    safeStringReturn(pathParam.getDefaultValue()), pathParam.getIn());
            if (pathParam.getType() != null && pathParam.getType().equals("array")) {
                Items items = new Items();
                items.setType(pathParam.getItems().getType());
                if (pathParam.getDefault() != null) {
                    items.setDefaultValue(pathParam.getDefault().toString());
                }

                rp.setItems(items);
            }
            if (pathParam.getDefaultValue() != null) {
                rp.setDefaultValue(pathParam.getDefaultValue().toString());
            }

            rp.setDescription(pathParam.getDescription());
            rp.setRequired(pathParam.getRequired());
            rp.setFormat(pathParam.getFormat());
            rp.setMaximum(pathParam.getMaximum());
            rp.setMinimum(pathParam.getMinimum());
            rp.setEnums(pathParam.getEnum());
            return rp;

        }
    }

    public StringProperty setDefaults(ResourceParameter param) {
        StringProperty pr = new StringProperty();
        pr.setType(param.getItems().getType());
        if (param.getItems().getEnums() != null && !param.getItems().getEnums().isEmpty()) {
            List<String> enums = new ArrayList<>();
            param.getEnums().forEach(e -> {
                enums.add(e.trim());
            });
            pr.setEnum(enums);
        }
        pr.setDefault(param.getItems().getDefaultValue());
        return pr;
    }

    public String safeStringReturn(Object obj) {
        return obj != null ? obj.toString() : null;
    }

}

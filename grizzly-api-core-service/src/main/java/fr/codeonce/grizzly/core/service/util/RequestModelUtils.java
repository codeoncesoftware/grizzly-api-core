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

import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import fr.codeonce.grizzly.core.domain.endpointModel.ModelProperty;
import fr.codeonce.grizzly.core.domain.resource.ResourceParameter;

import java.util.List;
import java.util.Optional;

public class RequestModelUtils {


    private RequestModelUtils() {
    }

    private static void generateFinalBody(int index, String[] finalBody, ResourceParameter p) {
        if (p.getType().equalsIgnoreCase("string")) {
            finalBody[index] += "\"" + p.getName() + "\":\"" + "\",";
        } else if (p.getType().equalsIgnoreCase("number")) {
            finalBody[index] += "\"" + p.getName() + "\":" + 1.0 + ",";
        } else if (p.getType().equalsIgnoreCase("boolean")) {
            finalBody[index] += "\"" + p.getName() + "\":" + true + ",";
        } else if (p.getType().equalsIgnoreCase("integer")) {
            finalBody[index] += "\"" + p.getName() + "\":" + 1 + ",";
        } else if (p.getType().equalsIgnoreCase("char")) {
            finalBody[index] += "\"" + p.getName() + "\":'" + "a" + "',";
        }

    }

    public static String collectApiParameters(String body, List<ResourceParameter> list) {

        final String[] finalBody = new String[]{"{", "\"query\":{", "\"pathVariable\":{", body};

        list.forEach((p) -> {
            if (p.getIn().equalsIgnoreCase("query")) {
                generateFinalBody(1, finalBody, p);

            } else if (p.getIn().equalsIgnoreCase("path")) {
                generateFinalBody(2, finalBody, p);

            } else if (p.getIn().equalsIgnoreCase("Body") && p.getType().equalsIgnoreCase("array")) {
                finalBody[3] = "\"body\":[" + finalBody[3].substring(0, finalBody[3].length() - 1).substring(7) + "],";
            }

        });

        if (finalBody[1].length() > 9) {
            finalBody[1] = finalBody[1].substring(0, finalBody[1].length() - 1);
        }
        if (finalBody[2].length() > "\"pathVariable:{\"".length()) {
            finalBody[2] = finalBody[2].substring(0, finalBody[2].length() - 1);
        }

        finalBody[1] = finalBody[1].concat("},");
        finalBody[2] = finalBody[2].concat("},");

        finalBody[0] = finalBody[0].concat(finalBody[1]).concat(finalBody[2])
                .concat(finalBody[3].substring(0, finalBody[3].length() - 1)).concat("}");

        return finalBody[0];

    }

    public static String parseEndpointModelsToBodyRequest(List<EndpointModel> modelsToAdd, String modelName,
                                                          String modelTitle, boolean firstTime) {
        Optional<EndpointModel> model;
        String body;
        if (firstTime) {
            body = "\"" + "body" + "\":{";

        } else {
            body = "\"" + modelName + "\":{";
        }

        if (modelTitle == null) {
            model = modelsToAdd.stream().filter((e) -> e.getTitle().equalsIgnoreCase(modelName)).findFirst();
        } else {
            model = modelsToAdd.stream().filter((e) -> e.getTitle().equalsIgnoreCase(modelTitle)).findFirst();
        }

        if (model.isPresent()) {

            for (ModelProperty e1 : model.get().getProperties()) {
                if (e1.getType().equalsIgnoreCase("string") && e1.getArray() == false) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":\"\",");
                } else if (e1.getType().equalsIgnoreCase("string") && e1.getArray() == true) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":[\"\"],");
                } else if (e1.getType().equalsIgnoreCase("number") && e1.getArray() == false) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":4.0,");
                } else if (e1.getType().equalsIgnoreCase("number") && e1.getArray() == true) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":[4.0],");
                } else if (e1.getType().equalsIgnoreCase("boolean") && e1.getArray() == false) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":true,");
                } else if (e1.getType().equalsIgnoreCase("boolean") && e1.getArray() == true) {
                    body = body.concat("\"").concat(e1.getName()).concat("\":[true],");
                } else {
                    String parseEndpointModelsToBodyRequest = parseEndpointModelsToBodyRequest(modelsToAdd,
                            e1.getName(), e1.getType(), false);
                    if (parseEndpointModelsToBodyRequest != null) {
                        if (e1.getArray() == false) {
                            body = body.concat(parseEndpointModelsToBodyRequest);

                        } else {
                            body = body.concat("[" + parseEndpointModelsToBodyRequest + "],");

                        }
                    }
                }

            }

            return body.substring(0, body.length() - 1).concat("},");
        }

        return null;

    }

}

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
package fr.codeonce.grizzly.core.domain.resource;

import java.util.Map;

public class APIResponse {

    private String code;
    private String description;
    private Headers headers;
    private APISchema schema;
    private Map<String, Object> exemples;
    private Object example;


    public APIResponse() {

    }


    public Object getExample() {
        return example;
    }


    public void setExample(Object example) {
        this.example = example;
    }


    public Map<String, Object> getExemples() {
        return exemples;
    }


    public void setExemples(Map<String, Object> exemples) {
        this.exemples = exemples;
    }


    public APIResponse(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public APISchema getSchema() {
        return schema;
    }

    public void setSchema(APISchema schema) {
        this.schema = schema;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

}

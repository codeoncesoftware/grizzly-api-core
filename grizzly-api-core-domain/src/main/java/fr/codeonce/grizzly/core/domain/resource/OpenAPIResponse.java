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

public class OpenAPIResponse {
    private String code;
    private String description;
    private Map<String, ContentEelement> content;
    private Headers headers;
    private Map<String, ExampleObject> exemples;
    private Object example;


    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        this.example = example;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public Map<String, ExampleObject> getExemples() {
        return exemples;
    }

    public void setExemples(Map<String, ExampleObject> exemples) {
        this.exemples = exemples;
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

    public Map<String, ContentEelement> getContent() {
        return content;
    }

    public void setContent(Map<String, ContentEelement> content) {
        this.content = content;
    }

}

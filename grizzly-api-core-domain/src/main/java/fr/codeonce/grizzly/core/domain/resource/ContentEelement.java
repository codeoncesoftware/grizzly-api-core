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

public class ContentEelement {

    private APISchema schema;
    private Map<String, ExampleObject> examples;

    public APISchema getSchema() {
        return schema;
    }

    public void setSchema(APISchema schema) {
        this.schema = schema;
    }

    public Map<String, ExampleObject> getExamples() {
        return examples;
    }

    public void setExamples(Map<String, ExampleObject> examples) {
        this.examples = examples;
    }

}

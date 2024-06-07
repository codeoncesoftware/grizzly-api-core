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

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Json2Pojo {
    private static final Logger log = LoggerFactory.getLogger(Json2Pojo.class);

    public static String jsonSchema2Pojo(String json, String packageName) {
        String finalString = null;
        final GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public SourceType getSourceType() {
                return SourceType.JSON;
            }

            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false;
            }

            @Override
            public boolean isIncludeToString() {
                return false;
            }

            @Override
            public boolean isIncludeTypeInfo() {
                return false;

            }

            @Override
            public boolean isIncludeConstructorPropertiesAnnotation() {
                return false;

            }

            @Override
            public boolean isIncludeJsr303Annotations() {
                return false;

            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return false;

            }

            @Override
            public boolean isIncludeGeneratedAnnotation() {
                return false;
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return AnnotationStyle.NONE;
            }

        };

        final JCodeModel codeModel = new JCodeModel();
        final RuleFactory ruleFactory = new RuleFactory(config, new NoopAnnotator(), new SchemaStore());
        final SchemaMapper schemaMapper = new SchemaMapper(ruleFactory, new SchemaGenerator());

        try {
            schemaMapper.generate(codeModel, "Request", packageName, json);
        } catch (IOException e) {
            log.error("" + e);
        }

        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            codeModel.build(new SingleStreamCodeWriter(s));
            finalString = new String(s.toByteArray());

        } catch (IOException e) {
            log.error("" + e);
        }

        String[] result = finalString.split(
                "-----------------------------------.*-----------------------------------[\\s\\nA-Za-z0-9\\.-;]*public");

        result[result.length - 1] = "public " + result[result.length - 1];
        return String.join("", result);

    }
}

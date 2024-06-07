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

package fr.codeonce.grizzly.core;

import com.itextpdf.text.DocumentException;
import fr.codeonce.grizzly.core.domain.config.AppProperties;
import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.FileNotFoundException;
//import fr.codeonce.grizzly.core.rest.config.security.JwtProperties;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class, MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@EnableCaching(proxyTargetClass = true)
@EnableConfigurationProperties(value = {AppProperties.class, MongoProperties.class, GrizzlyCoreProperties.class,
})
@EnableFeignClients
public class GrizzlyCoreApplication {

    public static void main(String[] args) throws DocumentException, FileNotFoundException {

        SpringApplication.run(GrizzlyCoreApplication.class, args);

    }

}

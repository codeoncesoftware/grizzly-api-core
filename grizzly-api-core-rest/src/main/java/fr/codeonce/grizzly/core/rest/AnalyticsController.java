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
package fr.codeonce.grizzly.core.rest;

import fr.codeonce.grizzly.common.runtime.HealthCheck;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsDto;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@CrossOrigin
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsDto getAnalytics() {
        return analyticsService.getAnalytics();
    }

    @GetMapping("/check")
    public Document checkUserLimits() {
        return analyticsService.checkUserLimits();
    }

    @GetMapping("/healthCheck")
    public HealthCheck getHealthCheck(@RequestParam String containerId) throws SQLException {
        return analyticsService.getHealthCheck(containerId);
    }


}

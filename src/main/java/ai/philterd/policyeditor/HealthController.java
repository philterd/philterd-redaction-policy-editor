/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.policyeditor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code GET /api/health} - liveness probe carrying the application version, matching the contract
 * shared across Philterd products. Answering at all means the application is serving, so the status
 * is always {@code UP}; a process that cannot serve returns no 200 for the probe to read.
 *
 * <p>This is not the actuator health endpoint. {@code /actuator/health} remains, and is what
 * aggregates the Spring health indicators and backs the Kubernetes-style liveness and readiness
 * probes; this endpoint is the cross-product contract, and is the one that reports the version.
 */
@RestController
public class HealthController {

    private final String applicationVersion;

    /**
     * The version comes from META-INF/build-info.properties, written by the build-info goal of the
     * Spring Boot Maven plugin. Running from classes built outside Maven (an IDE, for example) has
     * no such file and therefore no BuildProperties bean, so the version reports as unknown rather
     * than failing startup.
     */
    public HealthController(final ObjectProvider<BuildProperties> buildProperties) {
        final BuildProperties build = buildProperties.getIfAvailable();
        this.applicationVersion = (build != null) ? build.getVersion() : "unknown";
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        // Ordered so "status" leads the response, as in the documented contract.
        final Map<String, String> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("applicationVersion", applicationVersion);
        return health;
    }

}

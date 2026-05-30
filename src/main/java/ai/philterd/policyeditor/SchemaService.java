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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Discovers, caches, and validates against the redaction policy JSON schemas bundled in this
 * application under {@code classpath:schemas/<version>/redaction-policy-schema.json}.
 *
 * <p>Each Phileas build understands exactly one schema version, but the editor bundles every
 * version it can author so the user can select between them. This service is the source of truth
 * for which versions are available.</p>
 */
@Service
public class SchemaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaService.class);

    private static final String SCHEMA_LOCATION_PATTERN = "classpath*:schemas/*/redaction-policy-schema.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    /** version -> raw schema JSON, ordered ascending by semantic version. */
    private final Map<String, String> rawSchemasByVersion = new TreeMap<>(versionComparator());

    /** version -> compiled schema for validation. */
    private final Map<String, JsonSchema> compiledSchemasByVersion = new LinkedHashMap<>();

    /**
     * The schema version supported by the bundled Phileas runtime. Configured here until the
     * phileas dependency exposes {@code PolicySchema.getSupportedSchemaVersion()}.
     */
    @Value("${phileas.supported-schema-version:}")
    private String supportedTestVersion;

    @PostConstruct
    void load() throws IOException {
        final Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(SCHEMA_LOCATION_PATTERN);

        for (final Resource resource : resources) {
            final String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final JsonNode root = objectMapper.readTree(json);
            final JsonNode versionNode = root.get("version");
            if (versionNode == null || versionNode.isNull()) {
                LOGGER.warn("Skipping schema without a \"version\": {}", resource.getDescription());
                continue;
            }
            final String version = versionNode.asText();
            rawSchemasByVersion.put(version, json);
            compiledSchemasByVersion.put(version, schemaFactory.getSchema(json));
            LOGGER.info("Loaded redaction policy schema version {}", version);
        }

        if (rawSchemasByVersion.isEmpty()) {
            LOGGER.error("No redaction policy schemas were found on the classpath ({})", SCHEMA_LOCATION_PATTERN);
        }
    }

    /** All available schema versions, ascending by semantic version. */
    public List<String> getVersions() {
        return new ArrayList<>(rawSchemasByVersion.keySet());
    }

    /** The newest available schema version, or {@code null} if none are bundled. */
    public String getLatestVersion() {
        String latest = null;
        for (final String version : rawSchemasByVersion.keySet()) {
            latest = version; // ascending order, so the last key is the newest
        }
        return latest;
    }

    /** The schema version supported by the live Phileas runtime (used to gate the Test feature). */
    public String getSupportedTestVersion() {
        return supportedTestVersion;
    }

    public boolean hasVersion(final String version) {
        return rawSchemasByVersion.containsKey(version);
    }

    /** The raw schema JSON for a version, or {@code null} if the version is not bundled. */
    public String getSchemaJson(final String version) {
        return rawSchemasByVersion.get(version);
    }

    /**
     * Validates a policy JSON document against the schema for the given version.
     *
     * @return an ordered list of human-readable validation messages; empty when the policy is valid.
     * @throws IllegalArgumentException if the version is unknown or the policy is not parseable JSON.
     */
    public List<String> validate(final String version, final String policyJson) {
        final JsonSchema schema = compiledSchemasByVersion.get(version);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown schema version: " + version);
        }
        final JsonNode policyNode;
        try {
            policyNode = objectMapper.readTree(policyJson);
        } catch (final IOException e) {
            throw new IllegalArgumentException("The policy is not valid JSON: " + e.getMessage(), e);
        }
        // "$schema" is a JSON Schema keyword used to link a document to its schema for editor
        // tooling; it is not policy data. Strip it so additionalProperties:false does not flag it.
        if (policyNode.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) policyNode).remove("$schema");
        }
        final Set<ValidationMessage> messages = schema.validate(policyNode);
        final List<String> result = new ArrayList<>();
        for (final ValidationMessage message : messages) {
            result.add(message.getMessage());
        }
        return result;
    }

    /** Compares dotted version strings (e.g. "1.0.0" < "1.2.0" < "10.0.0") numerically. */
    private static Comparator<String> versionComparator() {
        return (a, b) -> {
            final String[] pa = a.split("\\.");
            final String[] pb = b.split("\\.");
            final int len = Math.max(pa.length, pb.length);
            for (int i = 0; i < len; i++) {
                final int va = i < pa.length ? parseOrZero(pa[i]) : 0;
                final int vb = i < pb.length ? parseOrZero(pb[i]) : 0;
                if (va != vb) {
                    return Integer.compare(va, vb);
                }
            }
            return a.compareTo(b);
        };
    }

    private static int parseOrZero(final String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }
}

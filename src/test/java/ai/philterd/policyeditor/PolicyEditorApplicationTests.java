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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PolicyEditorApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private SchemaService schemaService;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<String> jsonEntity(String body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** Escapes a string for embedding in a JSON request body built by hand. */
    private String quoteJson(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private HttpEntity<String> textEntity(String body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(body, headers);
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void schemaServiceLoadsTheSingleBundledVersion() {
        // The editor authors exactly one schema version: the one the bundled Phileas can run.
        assertThat(schemaService.getVersion()).isEqualTo("1.1.0");
        assertThat(schemaService.getSupportedTestVersion()).isEqualTo("1.1.0");
        assertThat(schemaService.getSchemaJson("1.1.0")).contains("\"title\": \"Phileas Redaction Policy\"");
        assertThat(schemaService.hasVersion("1.0.0")).isFalse();
        assertThat(schemaService.hasVersion("1.2.0")).isFalse();
    }

    @Test
    public void bundledSchemaIsTheOneItClaimsToBe() {
        // 1.1.0 carries regex validators and the local GLiNER model path, and does not yet carry
        // span disambiguation, which arrived in 1.2.0. Assert on the schema text so a mis-copied
        // schema file fails the build.
        assertThat(schemaService.getSchemaJson("1.1.0")).contains("\"validator\"");
        assertThat(schemaService.getSchemaJson("1.1.0")).contains("\"modelPath\"");
        assertThat(schemaService.getSchemaJson("1.1.0")).doesNotContain("spanDisambiguation");
    }

    @Test
    public void bundledCryptoSchemaDoesNotRequireAnInitializationVector() {
        // Per-value AES-GCM nonces replaced the configured "iv".
        final String policy = "{\"crypto\":{\"key\":\"env:CRYPTO_KEY\"},\"identifiers\":{}}";
        assertThat(schemaService.validate("1.1.0", policy)).isEmpty();
    }

    @Test
    public void shouldExposeHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/actuator/health"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    public void shouldExposePrometheusMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/actuator/prometheus"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // Prometheus text format, not JSON.
        assertThat(response.getBody()).contains("# TYPE");
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    public void shouldNotExposeOtherActuatorEndpoints() {
        // Only health and prometheus are exposed, and the discovery index is off, so every other
        // actuator path (including /actuator itself) must 404.
        for (final String path : List.of("", "/env", "/beans", "/mappings", "/loggers", "/heapdump")) {
            try {
                restTemplate.getForEntity(getUrl("/actuator" + path), String.class);
                throw new AssertionError("Expected 404 for /actuator" + path);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Test
    public void shouldReturnIndexPage() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Philterd Redaction Policy Editor");
        assertThat(response.getBody()).contains("Do not enter any PII.");
        assertThat(response.getBody()).contains("Version");
        assertThat(response.getBody()).contains("commit");
        assertThat(response.getBody()).contains("policy schema");
        assertThat(response.getBody()).contains("policy-form.js");
        // The test output is tabbed: redacted text with highlights, and the explain response.
        assertThat(response.getBody()).contains("test-output-tabs");
        assertThat(response.getBody()).contains("loadSampleBtn");
        assertThat(response.getBody()).contains("redacted-pane");
        assertThat(response.getBody()).contains("explanation-pane");
        assertThat(response.getBody()).doesNotContain("showExplanationBtn");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldListSchemas() {
        ResponseEntity<Map> response = restTemplate.getForEntity(getUrl("/api/schemas"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("version")).isEqualTo("1.1.0");
        assertThat(response.getBody().get("supportedTestVersion")).isEqualTo("1.1.0");
        // The multi-version selector is gone, so nothing advertises a list of versions.
        assertThat(response.getBody()).doesNotContainKey("versions");
    }

    @Test
    public void shouldServeSchemaForVersion() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/api/schemas/1.1.0"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"$defs\"");
        assertThat(response.getBody()).contains("baseFilterStrategy");
    }

    @Test
    public void shouldReturn404ForUnknownSchemaVersion() {
        try {
            restTemplate.getForEntity(getUrl("/api/schemas/9.9.9"), String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(404);
            return;
        }
        throw new AssertionError("Expected 404 for unknown schema version");
    }

    @Test
    public void shouldValidateConformingPolicy() {
        final String policy = "{\"identifiers\":{\"age\":{\"ageFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.1.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":true");
    }

    @Test
    public void shouldRejectNonConformingPolicy() {
        // "name" is not an allowed top-level property (additionalProperties: false).
        final String policy = "{\"name\":\"bad\",\"identifiers\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.1.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":false");
    }

    @Test
    public void shouldAcceptSchemaLinkInPolicy() {
        // $schema is a JSON Schema keyword and must not be treated as a forbidden additional property.
        final String policy = "{\"$schema\":\"https://www.philterd.ai/schemas/redaction-policy/1.1.0/schema.json\"," +
                "\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.1.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":true");
    }

    @Test
    public void shouldTestPolicyForSupportedVersion() {
        final String body = "{\"version\":\"1.1.0\",\"text\":\"My age is 25.\"," +
                "\"policy\":{\"identifiers\":{\"age\":{\"ageFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("filteredText");
        assertThat(response.getBody()).contains("explanation");
    }

    @Test
    public void shouldCompilePhiSqlToNativePolicy() {
        final String phiSql = "POLICY ssn_only;\nREDACT SSN WITH MASK;";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/compile"), textEntity(phiSql), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("ssn_only");
        // The compiled policy JSON (carried as a string field) is the native Phileas shape.
        assertThat(response.getBody()).contains("ssnFilterStrategies");
        assertThat(response.getBody()).contains("MASK");
        // PhiSQL 1.2.0 compiles to schema 1.1.0, the version this editor authors.
        assertThat(response.getBody()).contains("\"schemaVersion\":\"1.1.0\"");
    }

    @Test
    public void shouldRejectFeaturesFromNewerSchemaVersions() {
        // Span disambiguation arrived in schema 1.2.0, which this build does not author.
        final String policy = "{\"config\":{\"analysis\":{\"spanDisambiguation\":false}},\"identifiers\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.1.0"), jsonEntity(policy), String.class);
        assertThat(response.getBody()).contains("\"valid\":false");
    }

    @Test
    public void shouldValidateCompiledPhiSqlAgainstItsTargetSchema() {
        final String phiSql = "POLICY ssn_only;\nREDACT SSN WITH MASK;";
        ResponseEntity<Map> compiled = restTemplate.postForEntity(
                getUrl("/api/compile"), textEntity(phiSql), Map.class);
        final String policy = (String) compiled.getBody().get("policy");
        final String schemaVersion = (String) compiled.getBody().get("schemaVersion");

        ResponseEntity<String> validated = restTemplate.postForEntity(
                getUrl("/api/validate/" + schemaVersion), jsonEntity(policy), String.class);
        assertThat(validated.getStatusCode().value()).isEqualTo(200);
        assertThat(validated.getBody()).contains("\"valid\":true");
    }

    @Test
    public void shouldReturnErrorsForInvalidPhiSql() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/compile"), textEntity("this is not valid phisql"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"success\":false");
        assertThat(response.getBody()).contains("errors");
    }

    @Test
    public void shouldServeSyntheticSampleText() {
        // The samples exist so that testing a policy never requires the user to supply data of
        // their own, so every entry in the menu has to resolve.
        for (final String name : List.of("clinical-note", "financial-record", "legal-filing", "support-email")) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    getUrl("/samples/" + name + ".txt"), String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotBlank();
        }
    }

    @Test
    public void sampleTextShouldExerciseTheFilters() {
        // A sample that detects nothing would teach the user nothing, so check one end to end.
        final String sample = restTemplate.getForObject(getUrl("/samples/clinical-note.txt"), String.class);
        final String body = "{\"version\":\"1.1.0\",\"text\":" + quoteJson(sample) + "," +
                "\"policy\":{\"identifiers\":{" +
                "\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\"}]}," +
                "\"age\":{\"ageFilterStrategies\":[{\"strategy\":\"REDACT\"}]}," +
                "\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"REDACT\"}]}," +
                "\"phoneNumber\":{\"phoneNumberFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("REDACTED-ssn");
        assertThat(response.getBody()).contains("REDACTED-age");
        assertThat(response.getBody()).contains("REDACTED-email-address");
        assertThat(response.getBody()).contains("REDACTED-phone-number");
    }

    @Test
    public void shouldExplainSpansAcrossSeveralEntityTypes() {
        // The highlights are drawn from these spans, so the explanation has to carry one per
        // detection with the offsets and the replacement that shifted them.
        final String body = "{\"version\":\"1.1.0\"," +
                "\"text\":\"Patient John, SSN 123-45-6789, seen on 2026-01-15.\"," +
                "\"policy\":{\"identifiers\":{" +
                "\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\"}]}," +
                "\"firstName\":{\"firstNameFilterStrategies\":[{\"strategy\":\"MASK\"}]}," +
                "\"date\":{\"dateFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("appliedSpans");
        assertThat(response.getBody()).contains("identifiedSpans");
        assertThat(response.getBody()).contains("SSN").contains("FIRST_NAME").contains("DATE");
        // Replacements of a different length than the text they replace are what force the
        // highlight offsets to be remapped, so both shapes must be present.
        assertThat(response.getBody()).contains("REDACTED-ssn").contains("****");
    }

    @Test
    public void shouldReportSpansThatWereIdentifiedButNotApplied() {
        // An ignored value is detected and left in the clear. It appears among the identified spans
        // but not the applied ones, which is what lets the editor mark it differently.
        final String body = "{\"version\":\"1.1.0\",\"text\":\"SSN 123-45-6789 on 2026-01-15.\"," +
                "\"policy\":{\"identifiers\":{" +
                "\"ssn\":{\"ignored\":[\"123-45-6789\"],\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\"}]}," +
                "\"date\":{\"dateFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // The ignored SSN survives into the output; the date does not.
        assertThat(response.getBody()).contains("123-45-6789");
        assertThat(response.getBody()).contains("REDACTED-date");
        // The span carries ignored: true, and it is absent from appliedSpans. The editor keys off
        // the arrays rather than this flag, because an ignored span still reports applied: true.
        assertThat(response.getBody()).contains("\\\"ignored\\\": true");
    }

    @Test
    public void shouldExplainThatModelBackedFiltersCannotBeTested() {
        // PhEye detects with a model hosted elsewhere, and the editor calls no such service. The
        // user gets a sentence about it rather than a NullPointerException from inside Phileas.
        final String body = "{\"version\":\"1.1.0\",\"text\":\"John Smith lives in Boston.\"," +
                "\"policy\":{\"identifiers\":{\"pheyes\":[{" +
                "\"phEyeConfiguration\":{\"endpoint\":\"http://localhost:9999/\"}," +
                "\"phEyeFilterStrategies\":[{\"strategy\":\"REDACT\"}]}]}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("PhEye filter");
        assertThat(response.getBody()).contains("cannot be tested here");
        assertThat(response.getBody()).doesNotContain("HttpClient");
        assertThat(response.getBody()).doesNotContain("NullPointerException");
    }

    @Test
    public void shouldExplainThatTheDeprecatedPersonFilterCannotBeTestedEither() {
        // "person" is the deprecated spelling of the same PhEye filter.
        final String body = "{\"version\":\"1.1.0\",\"text\":\"John Smith lives in Boston.\"," +
                "\"policy\":{\"identifiers\":{\"person\":{" +
                "\"phEyeConfiguration\":{\"endpoint\":\"http://localhost:9999/\"}," +
                "\"phEyeFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);

        assertThat(response.getBody()).contains("PhEye filter");
        assertThat(response.getBody()).doesNotContain("HttpClient");
    }

    @Test
    public void shouldGateTestPolicyForUnsupportedVersion() {
        final String body = "{\"version\":\"9.9.9\",\"text\":\"My age is 25.\",\"policy\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("only available for schema version 1.1.0");
    }
}

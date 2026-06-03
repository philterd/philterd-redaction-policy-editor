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

    private HttpEntity<String> textEntity(String body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(body, headers);
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void schemaServiceLoadsBundledVersion() {
        assertThat(schemaService.getVersions()).contains("1.0.0");
        assertThat(schemaService.getLatestVersion()).isEqualTo("1.0.0");
        assertThat(schemaService.getSupportedTestVersion()).isEqualTo("1.0.0");
        assertThat(schemaService.getSchemaJson("1.0.0")).contains("\"title\": \"Phileas Redaction Policy\"");
    }

    @Test
    public void shouldReturnIndexPage() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Philterd Redaction Policy Editor");
        assertThat(response.getBody()).contains("Do not enter any PII.");
        assertThat(response.getBody()).contains("Version");
        assertThat(response.getBody()).contains("commit");
        assertThat(response.getBody()).contains("schema-version-select");
        assertThat(response.getBody()).contains("policy-form.js");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldListSchemas() {
        ResponseEntity<Map> response = restTemplate.getForEntity(getUrl("/api/schemas"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<String>) response.getBody().get("versions")).contains("1.0.0");
        assertThat(response.getBody().get("latest")).isEqualTo("1.0.0");
        assertThat(response.getBody().get("supportedTestVersion")).isEqualTo("1.0.0");
    }

    @Test
    public void shouldServeSchemaForVersion() {
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("/api/schemas/1.0.0"), String.class);
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
                getUrl("/api/validate/1.0.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":true");
    }

    @Test
    public void shouldRejectNonConformingPolicy() {
        // "name" is not an allowed top-level property (additionalProperties: false).
        final String policy = "{\"name\":\"bad\",\"identifiers\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.0.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":false");
    }

    @Test
    public void shouldAcceptSchemaLinkInPolicy() {
        // $schema is a JSON Schema keyword and must not be treated as a forbidden additional property.
        final String policy = "{\"$schema\":\"https://www.philterd.ai/schemas/redaction-policy/1.0.0/schema.json\"," +
                "\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/api/validate/1.0.0"), jsonEntity(policy), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"valid\":true");
    }

    @Test
    public void shouldTestPolicyForSupportedVersion() {
        final String body = "{\"version\":\"1.0.0\",\"text\":\"My age is 25.\"," +
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
    public void shouldGateTestPolicyForUnsupportedVersion() {
        final String body = "{\"version\":\"9.9.9\",\"text\":\"My age is 25.\",\"policy\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                getUrl("/test-policy"), jsonEntity(body), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("only available for schema version 1.0.0");
    }
}

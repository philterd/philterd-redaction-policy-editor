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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phisql.CompileResult;
import ai.philterd.phisql.Compiler;
import ai.philterd.phisql.PhiSQL;
import ai.philterd.phisql.PolicySchema;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import tools.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Controller
public class PolicyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyController.class);

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired(required = false)
    private GitProperties gitProperties;

    private final SchemaService schemaService;
    private final Gson gson;

    // Compiles PhiSQL policy source to native Phileas policy JSON. The compiler holds an immutable
    // catalog and compiles statelessly per call, so a single instance is reused.
    private final Compiler phiSqlCompiler = new Compiler();

    public PolicyController(final SchemaService schemaService) {
        this.schemaService = schemaService;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(new FilterStrategyTypeAdapterFactory())
                .create();
    }

    /**
     * Keeps the serialized policy clean by dropping strategy fields that do not apply to the
     * selected strategy (currently {@code anonymizationMethod}, which is only meaningful for
     * RANDOM_REPLACE). Retained from the previous implementation so the Test feature deserializes
     * policies the same way Phileas does.
     */
    private static class FilterStrategyTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!AbstractFilterStrategy.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonElement tree = delegate.toJsonTree(value);
                    if (tree.isJsonObject()) {
                        JsonObject jsonObject = tree.getAsJsonObject();
                        String strategy = "";
                        if (jsonObject.has("strategy")) {
                            strategy = jsonObject.get("strategy").getAsString();
                        }
                        if (!"RANDOM_REPLACE".equals(strategy)) {
                            jsonObject.remove("anonymizationMethod");
                        }
                    }
                    elementAdapter.write(out, tree);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    return delegate.read(in);
                }
            };
        }
    }

    @GetMapping("/")
    public String index(Model model) throws IOException {
        final String hidePiiWarning = System.getenv("HIDE_PII_WARNING");
        model.addAttribute("hidePiiWarning", "1".equals(hidePiiWarning));

        final String customHeaderFile = System.getenv("CUSTOM_HEADER_FILE");
        if (customHeaderFile != null) {
            final File file = new File(customHeaderFile);
            if (file.exists()) {
                final String customHeader = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                model.addAttribute("customHeader", customHeader);
            }
        }

        final String customFooterFile = System.getenv("CUSTOM_FOOTER_FILE");
        if (customFooterFile != null) {
            final File file = new File(customFooterFile);
            if (file.exists()) {
                final String customFooter = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                model.addAttribute("customFooter", customFooter);
            }
        }

        model.addAttribute("schemaVersion", schemaService.getVersion());
        model.addAttribute("supportedTestVersion", schemaService.getSupportedTestVersion());
        model.addAttribute("version", (buildProperties != null) ? buildProperties.getVersion() : "unknown");
        model.addAttribute("commit", (gitProperties != null) ? gitProperties.getShortCommitId() : "unknown");

        return "index";
    }

    /** Reports the schema version the editor authors, plus the version the Test feature can run. */
    @GetMapping("/api/schemas")
    @ResponseBody
    public Map<String, Object> schemas() {
        final Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", schemaService.getVersion());
        response.put("supportedTestVersion", schemaService.getSupportedTestVersion());
        return response;
    }

    /** Returns the raw JSON schema for a version so the browser can render the form from it. */
    @GetMapping("/api/schemas/{version}")
    @ResponseBody
    public ResponseEntity<String> schema(@PathVariable final String version) {
        final String json = schemaService.getSchemaJson(version);
        if (json == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    /** Validates a posted policy against the schema for the given version. */
    @PostMapping("/api/validate/{version}")
    @ResponseBody
    public ValidationResponse validate(@PathVariable final String version, @RequestBody final String policyJson) {
        try {
            final List<String> messages = schemaService.validate(version, policyJson);
            return new ValidationResponse(messages.isEmpty(), messages);
        } catch (final IllegalArgumentException e) {
            return new ValidationResponse(false, List.of(e.getMessage()));
        }
    }

    /**
     * Compiles a policy authored in PhiSQL into the native Phileas policy JSON the editor works with.
     * The request body is PhiSQL source. On success the response carries the policy name/description
     * from the {@code POLICY} declaration and the compiled policy JSON (as a string the UI parses and
     * loads into the form). Parse/compile errors are returned in {@code errors}.
     */
    @PostMapping("/api/compile")
    @ResponseBody
    public CompileResponse compile(@RequestBody final String phiSql) {
        if (phiSql == null || phiSql.isBlank()) {
            return CompileResponse.error(List.of("The PhiSQL is empty."));
        }

        final CompileResult result;
        try {
            result = phiSqlCompiler.compile(phiSql);
        } catch (final PhiSQL.ParseException | Compiler.CompileException e) {
            return CompileResponse.error(List.of(e.getMessage()));
        } catch (final Exception e) {
            LOGGER.error("Error compiling PhiSQL: {}", e.getMessage(), e);
            return CompileResponse.error(List.of("Failed to compile PhiSQL: " + e.getMessage()));
        }

        final String policyJson = result.toJsonString();

        // Validate the compiled policy against the schema before accepting it, so the editor never loads
        // a non-conforming policy. PhiSQL compiles to a specific schema version; validate against that.
        // If that version is not bundled the policy cannot be checked at all, and the editor has no
        // schema to render it with, so fail rather than pass an unvalidated policy through.
        final String schemaVersion = PolicySchema.getSupportedSchemaVersion();
        if (!schemaService.hasVersion(schemaVersion)) {
            LOGGER.error("The PhiSQL compiler targets schema version {}, but this build bundles {}",
                    schemaVersion, schemaService.getVersion());
            return CompileResponse.error(List.of("The PhiSQL compiler targets redaction policy schema version "
                    + schemaVersion + ", but this build authors version " + schemaService.getVersion() + "."));
        }

        final List<String> messages = schemaService.validate(schemaVersion, policyJson);
        if (!messages.isEmpty()) {
            return CompileResponse.error(messages);
        }

        return CompileResponse.ok(result.policyName(), result.description(), policyJson, schemaVersion);
    }

    /**
     * Runs the live Phileas engine over the supplied text using the posted policy. The policy is
     * built entirely in the browser, so this endpoint simply deserializes and applies it. Only
     * available when the requested version matches the version the bundled Phileas runtime supports.
     */
    @PostMapping("/test-policy")
    @ResponseBody
    public TestPolicyResponse testPolicy(@RequestBody final TestPolicyRequest request) {
        final String supported = schemaService.getSupportedTestVersion();
        if (request.getVersion() != null && supported != null && !supported.equals(request.getVersion())) {
            return new TestPolicyResponse(
                    "Testing is only available for schema version " + supported
                            + ", which is the version supported by this build's Phileas runtime.", "");
        }

        LOGGER.info("Testing policy against schema version {}", request.getVersion());
        try {
            final String policyJson = request.getPolicy() != null ? request.getPolicy().toString() : "{}";
            final Policy policy = gson.fromJson(policyJson, Policy.class);

            final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
            final PlainTextFilterService filterService = new PlainTextFilterService(
                    phileasConfiguration, new DefaultContextService(), new InMemoryVectorService(), null);

            final TextFilterResult result = filterService.filter(policy, "context", request.getText());

            return new TestPolicyResponse(result.getFilteredText(), gson.toJson(result.getExplanation()));
        } catch (final Exception e) {
            LOGGER.error("Error testing policy: {}", e.getMessage(), e);
            return new TestPolicyResponse("Error: " + e.getMessage(), "");
        }
    }

    /** Request body for {@link #testPolicy}. {@code policy} is the raw policy JSON built by the UI. */
    public static class TestPolicyRequest {
        private String version;
        private JsonNode policy;
        private String text;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public JsonNode getPolicy() {
            return policy;
        }

        public void setPolicy(JsonNode policy) {
            this.policy = policy;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class ValidationResponse {
        private final boolean valid;
        private final List<String> messages;

        public ValidationResponse(boolean valid, List<String> messages) {
            this.valid = valid;
            this.messages = messages;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    public static class TestPolicyResponse {
        private final String filteredText;
        private final String explanation;

        public TestPolicyResponse(String filteredText, String explanation) {
            this.filteredText = filteredText;
            this.explanation = explanation;
        }

        public String getFilteredText() {
            return filteredText;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    /** Response for {@link #compile}. On success carries the compiled native policy JSON as a string. */
    public static class CompileResponse {
        private final boolean success;
        private final String name;
        private final String description;
        private final String policy;
        // The schema version the PhiSQL compiler targets, so the UI can switch the form to it.
        private final String schemaVersion;
        private final List<String> errors;

        private CompileResponse(boolean success, String name, String description, String policy,
                                String schemaVersion, List<String> errors) {
            this.success = success;
            this.name = name;
            this.description = description;
            this.policy = policy;
            this.schemaVersion = schemaVersion;
            this.errors = errors;
        }

        static CompileResponse ok(String name, String description, String policy, String schemaVersion) {
            return new CompileResponse(true, name, description, policy, schemaVersion, List.of());
        }

        static CompileResponse error(List<String> errors) {
            return new CompileResponse(false, null, null, null, null, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getPolicy() {
            return policy;
        }

        public String getSchemaVersion() {
            return schemaVersion;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}

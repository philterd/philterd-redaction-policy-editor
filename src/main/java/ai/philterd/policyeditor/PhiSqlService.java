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

import ai.philterd.phisql.CompileResult;
import ai.philterd.phisql.Compiler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Converts a policy to PhiSQL and proves the conversion before handing it over.
 *
 * <p>The emitter is the reverse of the compiler, written here rather than in {@code phisql}, so
 * nothing guarantees on its own that the two agree. What does guarantee it is running them against
 * each other: the emitted PhiSQL is compiled back with the bundled compiler and the result compared
 * to the policy that went in. A construct the emitter cannot write shows up as a difference, and the
 * caller is told which part of the policy it was rather than being handed PhiSQL that quietly means
 * something else.</p>
 */
@Service
public class PhiSqlService {

    private final PhiSqlEmitter emitter;
    private final PhiSqlCatalog catalog;
    private final Compiler compiler = new Compiler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PhiSqlService(final PhiSqlEmitter emitter, final PhiSqlCatalog catalog) {
        this.emitter = emitter;
        this.catalog = catalog;
    }

    /** The emitted PhiSQL, or the reasons it could not be produced faithfully. */
    public record Result(String phiSql, List<String> differences) {

        public boolean faithful() {
            return differences.isEmpty();
        }
    }

    /**
     * Emits PhiSQL for a policy and verifies it round-trips.
     *
     * @param policyJson the policy to convert.
     * @param policyName name for the POLICY declaration.
     */
    public Result toPhiSql(final String policyJson, final String policyName) {
        final JsonNode policy;
        try {
            policy = objectMapper.readTree(policyJson);
        } catch (final Exception e) {
            return new Result(null, List.of("The policy is not valid JSON."));
        }

        stripSchemaLink(policy);

        // Some shapes have no PhiSQL form at all, and the field-by-field difference report explains
        // them poorly ("cannot express identifiers.age" is true but unhelpful). Name them first.
        final List<String> unwritable = unwritableShapes(policy);
        if (!unwritable.isEmpty()) {
            return new Result(null, unwritable);
        }

        // The WHERE clause is what a person writes, so try it first. Where the bundled compiler
        // cannot round-trip it, fall back to the argument form, which compiles to the same policy
        // and is still valid PhiSQL. Both attempts are verified; neither is guessed at.
        final Attempt idiomatic = attempt(policy, policyName, false);
        if (idiomatic.differences().isEmpty()) {
            return new Result(idiomatic.phiSql(), List.of());
        }

        final Attempt fallback = attempt(policy, policyName, true);
        if (fallback.differences().isEmpty()) {
            return new Result(fallback.phiSql(), List.of());
        }

        // Report the idiomatic attempt's differences: they describe the policy, not the workaround.
        return new Result(null, idiomatic.differences());
    }

    /**
     * Policy shapes PhiSQL cannot write, reported in terms of what the author would do about it.
     *
     * <p>A filter with an empty list of strategies is the common one. {@code REDACT AGE;} without a
     * strategy compiles to a policy with no age filter at all, so the filter cannot survive the
     * trip, and an empty list is not the same as an absent one to Phileas.</p>
     */
    private List<String> unwritableShapes(final JsonNode policy) {
        final List<String> problems = new ArrayList<>();

        final JsonNode identifiers = policy.path("identifiers");
        final Iterator<String> fields = identifiers.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            final PhiSqlCatalog.Entity entity = catalog.entityForField(field);
            if (entity == null) {
                continue;
            }
            final JsonNode strategies = identifiers.path(field).path(entity.phileasStrategiesField());
            if (strategies.isArray() && strategies.isEmpty()) {
                problems.add("The " + entity.name() + " filter has no redaction strategy, and PhiSQL "
                        + "cannot write a filter without one. Add a strategy to it, or remove the "
                        + "filter, then export again.");
            }
        }

        for (final JsonNode identifier : identifiers.path("identifiers")) {
            if (!identifier.hasNonNull("pattern") || identifier.path("pattern").asText().isBlank()) {
                final String name = identifier.path("classification").asText("(unnamed)");
                problems.add("The custom identifier '" + name + "' has no pattern, and PhiSQL writes "
                        + "custom identifiers as a regular expression to match. Give it a pattern, or "
                        + "remove it, then export again.");
            }
        }

        return problems;
    }

    private record Attempt(String phiSql, List<String> differences) {
    }

    /** Emits one candidate and compares what it compiles back to against the original policy. */
    private Attempt attempt(final JsonNode policy, final String policyName, final boolean conditionsAsArguments) {
        final String phiSql = emitter.emit(policy, policyName, conditionsAsArguments);

        final CompileResult compiled;
        try {
            compiled = compiler.compile(phiSql);
        } catch (final Exception e) {
            return new Attempt(null, List.of(
                    "The generated PhiSQL did not compile, so it cannot be trusted to mean the same "
                            + "thing as the policy: " + e.getMessage()));
        }

        final JsonNode roundTripped = objectMapper.convertValue(compiled.policyJson(), JsonNode.class);
        final List<String> differences = new ArrayList<>();
        compare(policy, roundTripped, "", differences);
        return new Attempt(phiSql, differences);
    }

    private JsonNode stripSchemaLink(final JsonNode policy) {
        if (policy.isObject()) {
            ((ObjectNode) policy).remove("$schema");
        }
        return policy;
    }

    /**
     * Compares the policy that went in with the one that came back, reporting each difference in
     * terms of where it is in the policy rather than as a wall of JSON.
     */
    private void compare(final JsonNode expected, final JsonNode actual, final String path,
                         final List<String> differences) {
        if (differences.size() >= 10) {
            return; // Enough to act on. The rest would repeat the same story.
        }

        if (expected.isObject() && actual.isObject()) {
            final TreeSet<String> fields = new TreeSet<>();
            expected.fieldNames().forEachRemaining(fields::add);
            actual.fieldNames().forEachRemaining(fields::add);
            for (final String field : fields) {
                final String where = path.isEmpty() ? field : path + "." + field;
                if (!expected.has(field)) {
                    differences.add("PhiSQL adds " + where + ", which the policy does not have.");
                } else if (!actual.has(field)) {
                    differences.add("PhiSQL cannot express " + where + ".");
                } else {
                    compare(expected.path(field), actual.path(field), where, differences);
                }
            }
            return;
        }

        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                differences.add("PhiSQL produces " + actual.size() + " entries at " + path
                        + " where the policy has " + expected.size() + ".");
                return;
            }
            for (int i = 0; i < expected.size(); i++) {
                compare(expected.get(i), actual.get(i), path + "[" + i + "]", differences);
            }
            return;
        }

        if (!equalScalars(expected, actual)) {
            differences.add("PhiSQL changes " + path + " from " + expected.asText()
                    + " to " + actual.asText() + ".");
        }
    }

    /** Numbers may come back as a different numeric type without meaning anything different. */
    private boolean equalScalars(final JsonNode expected, final JsonNode actual) {
        if (expected.isNumber() && actual.isNumber()) {
            return expected.decimalValue().compareTo(actual.decimalValue()) == 0;
        }
        return expected.equals(actual);
    }

    /** A file name for a policy, used for the PhiSQL download. */
    public static String fileNameFor(final String policyName) {
        final String cleaned = (policyName == null || policyName.isBlank()) ? "policy"
                : policyName.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
        return cleaned + ".phisql";
    }
}

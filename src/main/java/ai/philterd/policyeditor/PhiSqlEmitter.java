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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Writes a redaction policy back out as PhiSQL.
 *
 * <p>This is the reverse of what the PhiSQL compiler does, and it is driven by the same catalog the
 * compiler reads, so the two stay in step. It does not attempt to reproduce a particular source
 * document: what it emits is one PhiSQL text among the many that compile to the given policy.</p>
 *
 * <p>The emitter does not decide on its own whether its output is correct. {@link PhiSqlService}
 * compiles what comes out of here and compares the result to the policy that went in, so a
 * construct this class does not know how to write is caught rather than silently dropped.</p>
 */
@Service
public class PhiSqlEmitter {

    /** Policy fields that are not entity filters and are handled separately. */
    private static final List<String> NON_ENTITY_FIELDS =
            List.of("identifiers", "dictionaries", "sections", "pheyes");

    private final PhiSqlCatalog catalog;

    /** Set per call by {@link #emit(JsonNode, String, boolean)}; this service is used single-threaded
     *  per request through {@link PhiSqlService}, which creates its own emitter state per call. */
    private boolean conditionsAsArguments;

    public PhiSqlEmitter(final PhiSqlCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Emits PhiSQL for a policy.
     *
     * @param policy the policy document.
     * @param policyName the name for the {@code POLICY} declaration, or {@code null} to omit it.
     */
    public String emit(final JsonNode policy, final String policyName) {
        return emit(policy, policyName, false);
    }

    /**
     * @param conditionsAsArguments write a strategy condition as a named argument instead of a
     *        {@code WHERE} clause. The clause reads better and is what a person would write, but
     *        PhiSQL before 1.3.0 compiled it to {@code conditions} rather than {@code condition},
     *        so a policy with a condition did not survive the round trip that way. The argument
     *        form compiles to the right field on every version.
     */
    public String emit(final JsonNode policy, final String policyName, final boolean conditionsAsArguments) {
        this.conditionsAsArguments = conditionsAsArguments;
        final StringBuilder phiSql = new StringBuilder();

        if (policyName != null && !policyName.isBlank()) {
            phiSql.append("POLICY ").append(identifier(policyName)).append(";\n\n");
        }

        emitConfiguration(policy, phiSql);
        emitFilters(policy.path("identifiers"), phiSql);
        emitCustomIdentifiers(policy.path("identifiers").path("identifiers"), phiSql);
        emitDictionaries(policy.path("identifiers").path("dictionaries"), phiSql);
        emitSections(policy.path("identifiers").path("sections"), phiSql);
        emitPhEyes(policy.path("identifiers").path("pheyes"), phiSql);
        emitGraphical(policy.path("graphical"), phiSql);
        emitPolicyWideIgnores(policy, phiSql);

        return phiSql.toString().trim() + "\n";
    }

    /** Policy-level secrets and the four configuration blocks. */
    private void emitConfiguration(final JsonNode policy, final StringBuilder phiSql) {
        final JsonNode crypto = policy.path("crypto");
        if (crypto.isObject() && crypto.hasNonNull("key")) {
            phiSql.append("CONFIGURE CRYPTO KEY FROM ENV ")
                    .append(string(environmentVariable(crypto.path("key").asText()))).append(";\n");
        }

        final JsonNode fpe = policy.path("fpe");
        if (fpe.isObject() && fpe.hasNonNull("key") && fpe.hasNonNull("tweak")) {
            phiSql.append("CONFIGURE FPE KEY FROM ENV ")
                    .append(string(environmentVariable(fpe.path("key").asText())))
                    .append(" TWEAK FROM ENV ")
                    .append(string(environmentVariable(fpe.path("tweak").asText()))).append(";\n");
        }

        final JsonNode config = policy.path("config");
        for (final Map.Entry<String, String> block : Map.of(
                "splitting", "SPLITTING", "pdf", "PDF",
                "postFilters", "POSTFILTERS", "analysis", "ANALYSIS").entrySet()) {
            final JsonNode settings = config.path(block.getKey());
            if (settings.isObject() && !settings.isEmpty()) {
                phiSql.append("CONFIGURE ").append(block.getValue()).append(" (")
                        .append(settingList(settings)).append(");\n");
            }
        }

        if (phiSql.length() > 0 && phiSql.charAt(phiSql.length() - 1) == '\n'
                && phiSql.toString().endsWith(";\n")) {
            phiSql.append("\n");
        }
    }

    /** One REDACT statement per strategy, plus the filter's own options and ignore terms. */
    private void emitFilters(final JsonNode identifiers, final StringBuilder phiSql) {
        if (!identifiers.isObject()) {
            return;
        }

        final Iterator<String> fields = identifiers.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            if (NON_ENTITY_FIELDS.contains(field)) {
                continue;
            }
            final PhiSqlCatalog.Entity entity = catalog.entityForField(field);
            if (entity == null) {
                continue; // Not an entity filter this catalog knows. Verification will catch it.
            }

            final JsonNode filter = identifiers.path(field);
            final JsonNode strategies = filter.path(entity.phileasStrategiesField());
            final String options = optionList(filter, entity.phileasStrategiesField());

            for (final JsonNode strategy : strategies) {
                phiSql.append("REDACT ").append(entity.name()).append(" WITH ")
                        .append(strategyExpression(strategy));
                if (!conditionsAsArguments && strategy.hasNonNull("condition")
                        && !strategy.path("condition").asText().isBlank()) {
                    phiSql.append(" WHERE ").append(strategy.path("condition").asText());
                }
                if (!options.isBlank()) {
                    phiSql.append("\n  OPTIONS (").append(options).append(")");
                }
                phiSql.append(";\n");
            }

            final JsonNode ignored = filter.path("ignored");
            if (ignored.isArray() && !ignored.isEmpty()) {
                phiSql.append("IGNORE TERMS (").append(stringList(ignored))
                        .append(") FOR ").append(entity.name()).append(";\n");
            }
        }
    }

    /** {@code DEFINE IDENTIFIER 'MRN' MATCHING '...' CASE INSENSITIVE WITH REDACT;} */
    private void emitCustomIdentifiers(final JsonNode identifiers, final StringBuilder phiSql) {
        for (final JsonNode identifier : identifiers) {
            if (!identifier.hasNonNull("pattern") || identifier.path("pattern").asText().isBlank()) {
                continue; // DEFINE IDENTIFIER requires MATCHING; verification reports the gap.
            }
            phiSql.append("\nDEFINE IDENTIFIER ").append(string(identifier.path("classification").asText()))
                    .append("\n  MATCHING ").append(string(identifier.path("pattern").asText()));
            if (identifier.hasNonNull("groupNumber")) {
                phiSql.append("\n  GROUP ").append(identifier.path("groupNumber").asInt());
            }
            if (identifier.hasNonNull("caseSensitive")) {
                phiSql.append("\n  CASE ")
                        .append(identifier.path("caseSensitive").asBoolean() ? "SENSITIVE" : "INSENSITIVE");
            }
            appendStrategiesAndOptions(identifier, "identifierFilterStrategies",
                    List.of("classification", "pattern", "groupNumber", "caseSensitive"), phiSql);
        }
    }

    /** {@code DEFINE DICTIONARY 'meds' TERMS (...) FUZZY SENSITIVITY medium WITH REDACT;} */
    private void emitDictionaries(final JsonNode dictionaries, final StringBuilder phiSql) {
        for (final JsonNode dictionary : dictionaries) {
            phiSql.append("\nDEFINE DICTIONARY ").append(string(dictionary.path("classification").asText()));
            if (dictionary.path("terms").isArray() && !dictionary.path("terms").isEmpty()) {
                phiSql.append("\n  TERMS (").append(stringList(dictionary.path("terms"))).append(")");
            }
            if (dictionary.path("fuzzy").asBoolean(false)) {
                phiSql.append("\n  FUZZY");
                if (dictionary.hasNonNull("sensitivity")) {
                    phiSql.append(" SENSITIVITY ").append(dictionary.path("sensitivity").asText());
                }
            }
            appendStrategiesAndOptions(dictionary, "customFilterStrategies",
                    List.of("classification", "terms", "fuzzy", "sensitivity"), phiSql);
        }
    }

    /** {@code DEFINE SECTION START '...' END '...' WITH REDACT;} */
    private void emitSections(final JsonNode sections, final StringBuilder phiSql) {
        for (final JsonNode section : sections) {
            phiSql.append("\nDEFINE SECTION")
                    .append("\n  START ").append(string(section.path("startPattern").asText()))
                    .append("\n  END ").append(string(section.path("endPattern").asText()));
            appendStrategiesAndOptions(section, "sectionFilterStrategies",
                    List.of("startPattern", "endPattern"), phiSql);
        }
    }

    /** {@code DETECT PHEYE LABELS ('PERSON') ENDPOINT '...' WITH REDACT;} */
    private void emitPhEyes(final JsonNode phEyes, final StringBuilder phiSql) {
        for (final JsonNode phEye : phEyes) {
            phiSql.append("\nDETECT PHEYE");
            final JsonNode configuration = phEye.path("phEyeConfiguration");
            if (configuration.path("labels").isArray() && !configuration.path("labels").isEmpty()) {
                phiSql.append(" LABELS (").append(stringList(configuration.path("labels"))).append(")");
            }
            if (configuration.hasNonNull("endpoint")) {
                phiSql.append("\n  ENDPOINT ").append(string(configuration.path("endpoint").asText()));
            }
            if (configuration.hasNonNull("modelPath")) {
                phiSql.append("\n  MODEL ").append(string(configuration.path("modelPath").asText()));
            }
            // Anything else on the configuration has no clause of its own and travels in OPTIONS.
            final String remainder = optionList(configuration, "labels", "endpoint", "modelPath");
            final String extra = remainder.isBlank() ? "" : "phEyeConfiguration = (" + remainder + ")";
            appendStrategiesAndOptions(phEye, "phEyeFilterStrategies",
                    List.of("phEyeConfiguration"), extra, phiSql);
        }
    }

    /**
     * Appends the {@code WITH <strategy>} clause and any leftover properties as {@code OPTIONS},
     * shared by the DEFINE and DETECT statements.
     */
    private void appendStrategiesAndOptions(final JsonNode node, final String strategiesField,
                                            final List<String> handled, final StringBuilder phiSql) {
        appendStrategiesAndOptions(node, strategiesField, handled, "", phiSql);
    }

    private void appendStrategiesAndOptions(final JsonNode node, final String strategiesField,
                                            final List<String> handled, final String extraOption,
                                            final StringBuilder phiSql) {
        final JsonNode strategies = node.path(strategiesField);
        if (strategies.isArray() && !strategies.isEmpty()) {
            phiSql.append("\n  WITH ").append(strategyExpression(strategies.get(0)));
        }

        final List<String> excluded = new ArrayList<>(handled);
        excluded.add(strategiesField);
        final List<String> options = new ArrayList<>();
        final String own = optionList(node, excluded.toArray(new String[0]));
        if (!own.isBlank()) {
            options.add(own);
        }
        if (!extraOption.isBlank()) {
            options.add(extraOption);
        }
        if (!options.isEmpty()) {
            phiSql.append("\n  OPTIONS (").append(String.join(", ", options)).append(")");
        }
        phiSql.append(";\n");
    }

    /** {@code CONFIGURE GRAPHICAL BOX (x = 10, y = 20, w = 100, h = 50);} */
    private void emitGraphical(final JsonNode graphical, final StringBuilder phiSql) {
        for (final JsonNode box : graphical.path("boundingBoxes")) {
            phiSql.append("\nCONFIGURE GRAPHICAL BOX (").append(settingList(box)).append(");\n");
        }
    }

    /** Policy-wide ignore lists and ignore patterns. */
    private void emitPolicyWideIgnores(final JsonNode policy, final StringBuilder phiSql) {
        for (final JsonNode ignored : policy.path("ignored")) {
            if (!ignored.path("terms").isArray()) {
                continue;
            }
            phiSql.append("\nIGNORE TERMS (").append(stringList(ignored.path("terms"))).append(")");
            final String options = optionList(ignored, "terms");
            if (!options.isBlank()) {
                phiSql.append(" OPTIONS (").append(options).append(")");
            }
            phiSql.append(";\n");
        }

        for (final JsonNode pattern : policy.path("ignoredPatterns")) {
            if (!pattern.hasNonNull("pattern")) {
                continue;
            }
            phiSql.append("\nIGNORE PATTERN ").append(string(pattern.path("pattern").asText()));
            final String options = optionList(pattern, "pattern");
            if (!options.isBlank()) {
                phiSql.append(" OPTIONS (").append(options).append(")");
            }
            phiSql.append(";\n");
        }
    }

    /** {@code MASK} or {@code MASK(maskCharacter = '#', salt = TRUE)}. */
    private String strategyExpression(final JsonNode strategy) {
        final String phileasEnum = strategy.path("strategy").asText();
        final PhiSqlCatalog.Strategy known = catalog.strategyForEnum(phileasEnum);
        if (known == null) {
            // Unknown to the catalog: emit the enum and let verification reject it.
            return phileasEnum;
        }

        final List<String> arguments = new ArrayList<>();
        final Iterator<String> fields = strategy.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            if ("strategy".equals(field)) {
                continue;
            }
            if ("condition".equals(field) && !conditionsAsArguments) {
                continue; // Written as a WHERE clause instead.
            }
            final PhiSqlCatalog.Argument argument = known.argumentForField(field);
            if (argument == null) {
                // Not a named argument in this catalog. The compiler passes an unrecognized argument
                // through under its own name, which is what a strategy property added to the schema
                // after this catalog was published needs. Verification confirms it survived.
                arguments.add(settingKey(field) + " = " + literal(strategy.path(field)));
            } else {
                arguments.add(argument.name() + " = " + argumentValue(argument, strategy.path(field)));
            }
        }

        return arguments.isEmpty() ? known.name() : known.name() + "(" + String.join(", ", arguments) + ")";
    }

    /** Enum arguments are bare lowercase words in PhiSQL; everything else is a literal. */
    private String argumentValue(final PhiSqlCatalog.Argument argument, final JsonNode value) {
        if ("enum".equals(argument.type()) && value.isTextual()) {
            return value.asText().toLowerCase();
        }
        return literal(value);
    }

    /** Every property of a node except the ones already emitted elsewhere. */
    private String optionList(final JsonNode node, final String... excluded) {
        final List<String> settings = new ArrayList<>();
        final Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            if ("ignored".equals(field) || List.of(excluded).contains(field)) {
                continue;
            }
            settings.add(settingKey(field) + " = " + literal(node.path(field)));
        }
        return String.join(", ", settings);
    }

    private String settingList(final JsonNode settings) {
        final List<String> entries = new ArrayList<>();
        final Iterator<String> fields = settings.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            entries.add(settingKey(field) + " = " + literal(settings.path(field)));
        }
        return String.join(", ", entries);
    }

    private String stringList(final JsonNode array) {
        final List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(string(v.asText())));
        return String.join(", ", values);
    }

    private String literal(final JsonNode value) {
        if (value.isBoolean()) {
            return value.asBoolean() ? "TRUE" : "FALSE";
        }
        if (value.isNumber()) {
            return value.asText();
        }
        if (value.isArray()) {
            final List<String> values = new ArrayList<>();
            ((ArrayNode) value).forEach(v -> values.add(literal(v)));
            return "[" + String.join(", ", values) + "]";
        }
        if (value.isObject()) {
            final List<String> entries = new ArrayList<>();
            final Iterator<String> fields = value.fieldNames();
            while (fields.hasNext()) {
                final String field = fields.next();
                entries.add(settingKey(field) + " = " + literal(value.path(field)));
            }
            return "(" + String.join(", ", entries) + ")";
        }
        return string(value.asText());
    }

    /** Secrets are written as {@code env:NAME} in a policy and as the bare name in PhiSQL. */
    private String environmentVariable(final String value) {
        return value.startsWith("env:") ? value.substring("env:".length()) : value;
    }

    /** A setting key is bare unless it collides with a PhiSQL keyword, as {@code pattern} does. */
    private String settingKey(final String field) {
        return catalog.isReservedKeyword(field) ? string(field) : field;
    }

    private String string(final String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** A policy name has to be a bare identifier in the POLICY declaration. */
    private String identifier(final String value) {
        final String cleaned = value.trim().replaceAll("[^A-Za-z0-9_]", "_");
        return cleaned.matches("^[0-9].*") ? "p_" + cleaned : cleaned;
    }
}

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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads the PhiSQL catalog that the {@code ai.philterd:phisql} artifact carries as resources, and
 * indexes it in the direction the emitter needs: from a Phileas policy field back to the PhiSQL that
 * produces it.
 *
 * <p>The compiler reads the same files in the other direction, so the two agree by construction. A
 * PhiSQL upgrade that adds an entity type or a strategy argument is picked up by bumping the
 * dependency, with no table to maintain here.</p>
 */
@Service
public class PhiSqlCatalog {

    private static final String ENTITY_TYPES = "/spec/v1.0/catalog/entity-types.yaml";
    private static final String STRATEGIES = "/spec/v1.0/catalog/strategies.yaml";
    private static final String KEYWORDS = "/spec/v1.0/catalog/keywords.yaml";

    /** A named argument of a strategy, as PhiSQL spells it. */
    public record Argument(String name, String phileasField, String type, List<String> enumValues) {
    }

    /** A strategy: its PhiSQL keyword, the Phileas enum it compiles to, and its arguments. */
    public record Strategy(String name, String phileasEnum, List<Argument> arguments) {

        Argument argumentForField(final String phileasField) {
            return arguments.stream().filter(a -> a.phileasField().equals(phileasField)).findFirst().orElse(null);
        }
    }

    /**
     * An entity type: its PhiSQL keyword, the two Phileas fields it maps to, and any earlier names of
     * the strategies field that the schema still accepts.
     */
    public record Entity(String name, String phileasField, String phileasStrategiesField,
                         List<String> phileasStrategiesFieldAliases) {
    }

    private final Map<String, Entity> entitiesByField = new LinkedHashMap<>();
    private final Map<String, Strategy> strategiesByEnum = new LinkedHashMap<>();
    private final Set<String> reservedKeywords = new HashSet<>();

    @PostConstruct
    void load() throws IOException {
        final YAMLMapper mapper = new YAMLMapper();

        final JsonNode entities = read(mapper, ENTITY_TYPES).path("entities");
        for (final JsonNode entity : entities) {
            final List<String> aliases = new ArrayList<>();
            entity.path("phileas_strategies_field_aliases").forEach(a -> aliases.add(a.asText()));
            final Entity e = new Entity(
                    entity.path("name").asText(),
                    entity.path("phileas_field").asText(),
                    entity.path("phileas_strategies_field").asText(),
                    aliases);
            entitiesByField.put(e.phileasField(), e);
        }

        final JsonNode strategies = read(mapper, STRATEGIES).path("strategies");
        for (final JsonNode strategy : strategies) {
            final List<Argument> arguments = new ArrayList<>();
            for (final JsonNode argument : strategy.path("args")) {
                final List<String> enumValues = new ArrayList<>();
                argument.path("enum_values").forEach(v -> enumValues.add(v.asText()));
                arguments.add(new Argument(
                        argument.path("name").asText(),
                        argument.path("phileas_field").asText(),
                        argument.path("type").asText(),
                        enumValues));
            }
            final Strategy s = new Strategy(
                    strategy.path("name").asText(),
                    strategy.path("phileas_enum").asText(),
                    arguments);
            // Several PhiSQL keywords can share a Phileas enum; the first one wins, which is the
            // canonical spelling in the catalog.
            strategiesByEnum.putIfAbsent(s.phileasEnum(), s);
        }

        read(mapper, KEYWORDS).path("keywords")
                .forEach(keyword -> reservedKeywords.add(keyword.asText().toUpperCase()));

        if (entitiesByField.isEmpty() || strategiesByEnum.isEmpty()) {
            throw new IllegalStateException("The PhiSQL catalog is missing from the classpath");
        }
    }

    private JsonNode read(final YAMLMapper mapper, final String resource) throws IOException {
        try (InputStream in = PhiSqlCatalog.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Not on the classpath: " + resource);
            }
            return mapper.readTree(in);
        }
    }

    /** The entity type whose filter occupies the given policy field, or {@code null}. */
    public Entity entityForField(final String phileasField) {
        return entitiesByField.get(phileasField);
    }

    /** The strategy that compiles to the given Phileas strategy enum, or {@code null}. */
    public Strategy strategyForEnum(final String phileasEnum) {
        return strategiesByEnum.get(phileasEnum);
    }

    /**
     * True when a name collides with a PhiSQL keyword, in which case it has to be quoted where the
     * grammar expects an identifier. {@code pattern} is the common case.
     */
    public boolean isReservedKeyword(final String name) {
        return reservedKeywords.contains(name.toUpperCase());
    }
}

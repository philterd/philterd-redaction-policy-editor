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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the policy to PhiSQL direction.
 *
 * <p>Each case asserts on the emitted text, because a person reads it, and relies on the service's
 * own verification for meaning: the service only returns PhiSQL that compiles back to the policy it
 * was given, so a returned result is a proof of equivalence rather than a claim about one.</p>
 */
@SpringBootTest
public class PhiSqlEmitterTests {

    @Autowired
    private PhiSqlService phiSqlService;

    private String emit(final String policy) {
        final PhiSqlService.Result result = phiSqlService.toPhiSql(policy, "test_policy");
        assertThat(result.differences()).isEmpty();
        return result.phiSql();
    }

    @Test
    public void shouldEmitAPolicyDeclarationAndAStrategy() {
        final String phiSql = emit("{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"MASK\"}]}}}");
        assertThat(phiSql).contains("POLICY test_policy;");
        assertThat(phiSql).contains("REDACT SSN WITH MASK;");
    }

    @Test
    public void shouldEmitStrategyArgumentsUnderTheirPhiSqlNames() {
        // maskCharacter is spelled mask_char in PhiSQL, which is why the catalog drives this.
        final String phiSql = emit("{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":" +
                "[{\"strategy\":\"MASK\",\"maskCharacter\":\"#\"}]}}}");
        assertThat(phiSql).contains("REDACT SSN WITH MASK(mask_char = '#');");
    }

    @Test
    public void shouldEmitOneStatementPerStrategy() {
        final String phiSql = emit("{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":" +
                "[{\"strategy\":\"HASH_SHA256_REPLACE\"},{\"strategy\":\"MASK\"}]}}}");
        assertThat(phiSql).contains("REDACT EMAIL_ADDRESS WITH HASH_SHA256;");
        assertThat(phiSql).contains("REDACT EMAIL_ADDRESS WITH MASK;");
    }

    @Test
    public void shouldEmitFilterPropertiesAsOptions() {
        final String phiSql = emit("{\"identifiers\":{\"creditCard\":{\"creditCardFilterStrategies\":" +
                "[{\"strategy\":\"MASK\"}],\"priority\":10,\"windowSize\":5}}}");
        assertThat(phiSql).contains("OPTIONS (");
        assertThat(phiSql).contains("priority = 10");
        assertThat(phiSql).contains("windowSize = 5");
    }

    @Test
    public void shouldEmitConditionsThatSurviveTheRoundTrip() {
        // PhiSQL 1.2.0 compiles a WHERE clause to "conditions" rather than "condition", so the
        // service falls back to the argument form. Either way the policy has to come back intact,
        // which the service verifies before returning anything.
        final String phiSql = emit("{\"identifiers\":{\"creditCard\":{\"creditCardFilterStrategies\":" +
                "[{\"strategy\":\"LAST_4\",\"condition\":\"confidence > 0.85\"}]}}}");
        assertThat(phiSql).contains("confidence > 0.85");
        assertThat(phiSql).contains("LAST_4");
    }

    @Test
    public void shouldEmitPolicyWideIgnoreListsAndPatterns() {
        final String phiSql = emit("{\"identifiers\":{},\"ignored\":[{\"name\":\"allow\",\"terms\":[\"Acme\"]}]," +
                "\"ignoredPatterns\":[{\"name\":\"four-digits\",\"pattern\":\"[0-9]{4}\"}]}");
        assertThat(phiSql).contains("IGNORE TERMS ('Acme') OPTIONS (name = 'allow');");
        // "pattern" collides with a PhiSQL keyword, so it has to be quoted as a setting key.
        assertThat(phiSql).contains("IGNORE PATTERN '[0-9]{4}' OPTIONS (name = 'four-digits');");
    }

    @Test
    public void shouldEmitFilterLevelIgnoreTerms() {
        final String phiSql = emit("{\"identifiers\":{\"firstName\":{\"ignored\":[\"Acme\"]," +
                "\"firstNameFilterStrategies\":[{\"strategy\":\"REDACT\"}]}}}");
        assertThat(phiSql).contains("IGNORE TERMS ('Acme') FOR FIRST_NAME;");
    }

    @Test
    public void shouldEmitConfigurationBlocks() {
        final String phiSql = emit("{\"identifiers\":{},\"crypto\":{\"key\":\"env:CRYPTO_KEY\"}," +
                "\"config\":{\"splitting\":{\"enabled\":true,\"threshold\":5000,\"method\":\"newline\"}}}");
        assertThat(phiSql).contains("CONFIGURE CRYPTO KEY FROM ENV 'CRYPTO_KEY';");
        assertThat(phiSql).contains("CONFIGURE SPLITTING (");
        assertThat(phiSql).contains("enabled = TRUE");
        assertThat(phiSql).contains("threshold = 5000");
    }

    @Test
    public void shouldEmitCustomIdentifiersDictionariesSectionsAndPhEye() {
        final String phiSql = emit("{\"identifiers\":{" +
                "\"identifiers\":[{\"classification\":\"MRN\",\"pattern\":\"MRN[0-9]+\",\"caseSensitive\":false," +
                "\"identifierFilterStrategies\":[{\"strategy\":\"REDACT\"}]}]," +
                "\"dictionaries\":[{\"classification\":\"meds\",\"terms\":[\"aspirin\"]," +
                "\"customFilterStrategies\":[{\"strategy\":\"REDACT\"}]}]," +
                "\"sections\":[{\"startPattern\":\"BEGIN\",\"endPattern\":\"END\"," +
                "\"sectionFilterStrategies\":[{\"strategy\":\"REDACT\"}]}]," +
                "\"pheyes\":[{\"phEyeConfiguration\":{\"labels\":[\"PER\"]}," +
                "\"phEyeFilterStrategies\":[{\"strategy\":\"REDACT\"}]}]}}");
        assertThat(phiSql).contains("DEFINE IDENTIFIER 'MRN'");
        assertThat(phiSql).contains("MATCHING 'MRN[0-9]+'");
        assertThat(phiSql).contains("CASE INSENSITIVE");
        assertThat(phiSql).contains("DEFINE DICTIONARY 'meds'");
        assertThat(phiSql).contains("DEFINE SECTION");
        assertThat(phiSql).contains("DETECT PHEYE LABELS ('PER')");
    }

    @Test
    public void shouldEmitGraphicalBoundingBoxes() {
        final String phiSql = emit("{\"identifiers\":{},\"graphical\":{\"boundingBoxes\":" +
                "[{\"x\":10,\"y\":20,\"w\":100,\"h\":50}]}}");
        assertThat(phiSql).contains("CONFIGURE GRAPHICAL BOX (x = 10, y = 20, w = 100, h = 50);");
    }

    @Test
    public void shouldSayWhatToDoAboutAFilterWithNoStrategy() {
        // A filter with an empty strategies list is what the form produces when the seeded strategy
        // is removed. "REDACT AGE;" compiles to a policy with no age filter, so the filter cannot
        // survive the trip, and an empty list is not the same as an absent one to Phileas.
        final PhiSqlService.Result result = phiSqlService.toPhiSql(
                "{\"identifiers\":{\"age\":{\"ageFilterStrategies\":[]}}}", "test_policy");

        assertThat(result.faithful()).isFalse();
        assertThat(result.differences()).hasSize(1);
        assertThat(result.differences().get(0))
                .contains("AGE filter has no redaction strategy")
                .contains("Add a strategy to it, or remove the filter");
    }

    @Test
    public void shouldSayWhatToDoAboutACustomIdentifierWithNoPattern() {
        final PhiSqlService.Result result = phiSqlService.toPhiSql("{\"identifiers\":{\"identifiers\":" +
                "[{\"classification\":\"account\",\"identifierFilterStrategies\":" +
                "[{\"strategy\":\"LAST_4\"}]}]}}", "test_policy");

        assertThat(result.faithful()).isFalse();
        assertThat(result.differences().get(0))
                .contains("custom identifier 'account' has no pattern")
                .contains("Give it a pattern");
    }

    @Test
    public void shouldReportAPolicyItCannotExpress() {
        // DEFINE IDENTIFIER requires a MATCHING pattern, so a custom identifier without one has no
        // PhiSQL form. The service says which part of the policy is the problem.
        final PhiSqlService.Result result = phiSqlService.toPhiSql("{\"identifiers\":{\"identifiers\":" +
                "[{\"classification\":\"account\",\"identifierFilterStrategies\":" +
                "[{\"strategy\":\"LAST_4\"}]}]}}", "test_policy");

        assertThat(result.faithful()).isFalse();
        assertThat(result.phiSql()).isNull();
        assertThat(result.differences()).anyMatch(d -> d.contains("custom identifier"));
    }

    @Test
    public void shouldRejectPolicyJsonThatIsNotJson() {
        final PhiSqlService.Result result = phiSqlService.toPhiSql("not json", "test_policy");
        assertThat(result.faithful()).isFalse();
        assertThat(result.differences()).containsExactly("The policy is not valid JSON.");
    }

    @Test
    public void shouldNameTheFileAfterThePolicy() {
        assertThat(PhiSqlService.fileNameFor("clinic_policy")).isEqualTo("clinic_policy.phisql");
        assertThat(PhiSqlService.fileNameFor(null)).isEqualTo("policy.phisql");
        assertThat(PhiSqlService.fileNameFor("a policy/name")).isEqualTo("a-policy-name.phisql");
    }
}

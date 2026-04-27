package ai.philterd.policyeditor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PolicyEditorApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void shouldGeneratePolicyWithCustomWindowSize() {
        PolicyRequest request = new PolicyRequest();
        request.setName("test-window-size");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("Age");
        selection.setWindowSize(10);
        PolicyRequest.StrategySelection strategy = new PolicyRequest.StrategySelection();
        strategy.setStrategy("REDACT");
        selection.getStrategies().add(strategy);
        request.getFilters().add(selection);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"windowSize\": 10");
    }

    @Test
    public void shouldReturnIndexPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("Philterd Redaction Policy Editor");
        assertThat(response.getBody()).contains("Do not enter any PII.");
    }

    @Test
    public void shouldGeneratePolicy() {
        PolicyRequest request = new PolicyRequest();
        request.setName("test-policy");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("Age");
        PolicyRequest.StrategySelection strategy = new PolicyRequest.StrategySelection();
        strategy.setStrategy("REDACT");
        selection.getStrategies().add(strategy);
        request.getFilters().add(selection);
        request.getPostFilters().setRemoveTrailingPeriods(true);
        request.getPostFilters().setRemoveTrailingSpaces(false);
        request.getPostFilters().setRemoveTrailingNewLines(true);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"strategy\": \"REDACT\"");
        assertThat(response.getBody()).contains("ageFilterStrategies");
        assertThat(response.getBody()).contains("\"windowSize\": 5");
        assertThat(response.getBody()).doesNotContain("anonymizationMethod");
        assertThat(response.getBody()).contains("\"removeTrailingPeriods\": true");
        assertThat(response.getBody()).contains("\"removeTrailingSpaces\": false");
        assertThat(response.getBody()).contains("\"removeTrailingNewLines\": true");
    }
    @Test
    public void shouldGenerateZipCodePolicyWithSpecificOptions() {
        PolicyRequest request = new PolicyRequest();
        request.setName("zip-policy");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("Zip Code");
        PolicyRequest.StrategySelection strategy = new PolicyRequest.StrategySelection();
        strategy.setStrategy("TRUNCATE");
        strategy.setTruncateDigits(3);
        strategy.setTruncateLeaveCharacters(2);
        selection.getStrategies().add(strategy);
        selection.setRequireDelimiter(true);
        selection.setValidate(true);
        request.getFilters().add(selection);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"strategy\": \"TRUNCATE\"");
        assertThat(response.getBody()).contains("zipCodeFilterStrategy");
        assertThat(response.getBody()).contains("\"truncateLeaveCharacters\": 3");
        assertThat(response.getBody()).contains("\"requireDelimiter\": true");
        assertThat(response.getBody()).contains("\"validate\": true");
    }

    @Test
    public void shouldGenerateComplexPolicyWithMultipleFilters() {
        PolicyRequest request = new PolicyRequest();
        request.setName("complex-policy");

        // First Name (Dynamic)
        PolicyRequest.FilterSelection fn = new PolicyRequest.FilterSelection();
        fn.setType("First Name");
        PolicyRequest.StrategySelection fnStrategy = new PolicyRequest.StrategySelection();
        fnStrategy.setStrategy("MASK");
        fn.getStrategies().add(fnStrategy);
        fn.setSensitivity("high");
        fn.setCapitalized(true);
        fn.setFuzzy(false);
        request.getFilters().add(fn);

        // Email Address
        PolicyRequest.FilterSelection email = new PolicyRequest.FilterSelection();
        email.setType("Email Address");
        PolicyRequest.StrategySelection emailStrategy = new PolicyRequest.StrategySelection();
        emailStrategy.setStrategy("REDACT");
        email.getStrategies().add(emailStrategy);
        email.setOnlyStrictMatches(true);
        email.setOnlyValidTLDs(false);
        request.getFilters().add(email);

        // Tracking Number
        PolicyRequest.FilterSelection tn = new PolicyRequest.FilterSelection();
        tn.setType("Tracking Number");
        PolicyRequest.StrategySelection tnStrategy = new PolicyRequest.StrategySelection();
        tnStrategy.setStrategy("REDACT");
        tn.getStrategies().add(tnStrategy);
        tn.setUps(true);
        tn.setFedex(false);
        tn.setUsps(true);
        tn.setAllowSpaces(true);
        request.getFilters().add(tn);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // Assertions for First Name
        assertThat(response.getBody()).contains("firstNameFilterStrategies");
        assertThat(response.getBody()).contains("\"sensitivity\": \"high\"");
        assertThat(response.getBody()).contains("\"capitalized\": true");

        // Assertions for Email
        assertThat(response.getBody()).contains("emailAddressFilterStrategies");
        assertThat(response.getBody()).contains("\"onlyStrictMatches\": true");

        // Assertions for Tracking Number
        assertThat(response.getBody()).contains("trackingNumberFilterStrategies");
        assertThat(response.getBody()).contains("\"ups\": true");
        assertThat(response.getBody()).contains("\"fedex\": false");
    }

    @Test
    public void shouldGeneratePolicyWithCondition() {
        PolicyRequest request = new PolicyRequest();
        request.setName("condition-policy");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("Age");
        PolicyRequest.StrategySelection strategy = new PolicyRequest.StrategySelection();
        strategy.setStrategy("REDACT");
        strategy.setCondition("confidence > 0.8");
        selection.getStrategies().add(strategy);
        request.getFilters().add(selection);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"strategy\": \"REDACT\"");
        assertThat(response.getBody()).contains("\"condition\": \"confidence > 0.8\"");
    }

    @Test
    public void shouldGeneratePolicyWithPdfAndSplittingSettings() {
        PolicyRequest request = new PolicyRequest();
        request.setName("config-policy");
        
        request.getPdf().setRedactionColor("red");
        request.getPdf().setDpi(300);
        request.getPdf().setShowReplacement(true);
        
        request.getSplitting().setEnabled(true);
        request.getSplitting().setMethod("sentence");
        request.getSplitting().setThreshold(500);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        
        assertThat(response.getBody()).contains("\"redactionColor\": \"red\"");
        assertThat(response.getBody()).contains("\"dpi\": 300");
        assertThat(response.getBody()).contains("\"showReplacement\": true");
        
        assertThat(response.getBody()).contains("\"enabled\": true");
        assertThat(response.getBody()).contains("\"method\": \"sentence\"");
        assertThat(response.getBody()).contains("\"threshold\": 500");
    }
    @Test
    public void shouldGeneratePolicyWithMultipleStrategiesForSameFilter() {
        PolicyRequest request = new PolicyRequest();
        request.setName("multi-strategy-policy");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("Age");
        
        PolicyRequest.StrategySelection s1 = new PolicyRequest.StrategySelection();
        s1.setStrategy("REDACT");
        s1.setCondition("confidence > 0.8");
        selection.getStrategies().add(s1);

        PolicyRequest.StrategySelection s2 = new PolicyRequest.StrategySelection();
        s2.setStrategy("MASK");
        s2.setCondition("confidence <= 0.8");
        selection.getStrategies().add(s2);

        request.getFilters().add(selection);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        
        assertThat(response.getBody()).contains("ageFilterStrategies");
        assertThat(response.getBody()).contains("\"strategy\": \"REDACT\"");
        assertThat(response.getBody()).contains("\"condition\": \"confidence > 0.8\"");
        assertThat(response.getBody()).contains("\"strategy\": \"MASK\"");
        assertThat(response.getBody()).contains("\"condition\": \"confidence <= 0.8\"");
    }

    @Test
    public void shouldIncludeAnonymizationMethodForRandomReplace() {
        PolicyRequest request = new PolicyRequest();
        request.setName("random-replace-policy");
        PolicyRequest.FilterSelection selection = new PolicyRequest.FilterSelection();
        selection.setType("First Name");
        PolicyRequest.StrategySelection strategy = new PolicyRequest.StrategySelection();
        strategy.setStrategy("RANDOM_REPLACE");
        selection.getStrategies().add(strategy);
        request.getFilters().add(selection);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"strategy\": \"RANDOM_REPLACE\"");
        assertThat(response.getBody()).contains("\"anonymizationMethod\": \"REALISTIC\"");
    }

    @Test
    public void shouldGeneratePolicyWithMultiplePhEyeFilters() {
        PolicyRequest request = new PolicyRequest();
        request.setName("pheye-policy");

        // First PhEye filter
        PolicyRequest.FilterSelection p1 = new PolicyRequest.FilterSelection();
        p1.setType("PhEye");
        p1.setEndpoint("http://pheye-1:8080");
        p1.setBearerToken("token-1");
        p1.setLabels(java.util.Arrays.asList("PER", "LOC"));
        PolicyRequest.StrategySelection s1 = new PolicyRequest.StrategySelection();
        s1.setStrategy("REDACT");
        p1.getStrategies().add(s1);
        request.getFilters().add(p1);

        // Second PhEye filter
        PolicyRequest.FilterSelection p2 = new PolicyRequest.FilterSelection();
        p2.setType("PhEye");
        p2.setEndpoint("http://pheye-2:8080");
        p2.setBearerToken("token-2");
        p2.setLabels(java.util.Arrays.asList("ORG"));
        PolicyRequest.StrategySelection s2 = new PolicyRequest.StrategySelection();
        s2.setStrategy("MASK");
        p2.getStrategies().add(s2);
        request.getFilters().add(p2);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        System.out.println("[DEBUG_LOG] PhEye Response: " + response.getBody());
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(response.getBody()).contains("pheyes");
        assertThat(response.getBody()).contains("http://pheye-1:8080");
        assertThat(response.getBody()).contains("token-1");
        assertThat(response.getBody()).contains("http://pheye-2:8080");
        assertThat(response.getBody()).contains("token-2");
        assertThat(response.getBody()).contains("\"strategy\": \"REDACT\"");
        assertThat(response.getBody()).contains("\"strategy\": \"MASK\"");
    }

    @Test
    public void shouldGeneratePolicyWithMultipleDictionaryFilters() {
        PolicyRequest request = new PolicyRequest();
        request.setName("dictionary-policy");

        // First Dictionary filter
        PolicyRequest.FilterSelection d1 = new PolicyRequest.FilterSelection();
        d1.setType("Dictionary");
        d1.setClassification("medical");
        d1.setSensitivity("high");
        d1.setCapitalized(true);
        d1.setFuzzy(false);
        PolicyRequest.StrategySelection s1 = new PolicyRequest.StrategySelection();
        s1.setStrategy("REDACT");
        d1.getStrategies().add(s1);
        request.getFilters().add(d1);

        // Second Dictionary filter
        PolicyRequest.FilterSelection d2 = new PolicyRequest.FilterSelection();
        d2.setType("Dictionary");
        d2.setClassification("legal");
        d2.setSensitivity("low");
        d2.setCapitalized(false);
        d2.setFuzzy(true);
        PolicyRequest.StrategySelection s2 = new PolicyRequest.StrategySelection();
        s2.setStrategy("MASK");
        d2.getStrategies().add(s2);
        request.getFilters().add(d2);

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);
        System.out.println("[DEBUG_LOG] Dictionary Response: " + response.getBody());
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(response.getBody()).contains("dictionaries");
        assertThat(response.getBody()).contains("\"classification\": \"medical\"");
        assertThat(response.getBody()).contains("\"sensitivity\": \"high\"");
        assertThat(response.getBody()).contains("\"capitalized\": true");
        assertThat(response.getBody()).contains("\"classification\": \"legal\"");
        assertThat(response.getBody()).contains("\"sensitivity\": \"low\"");
        assertThat(response.getBody()).contains("\"fuzzy\": true");
        assertThat(response.getBody()).contains("ADD YOUR TERMS HERE");
        assertThat(response.getBody()).contains("customFilterStrategies");
        assertThat(response.getBody()).contains("\"strategy\": \"REDACT\"");
        assertThat(response.getBody()).contains("\"strategy\": \"MASK\"");
    }
    @Test
    public void shouldGeneratePolicyWithDisabledFilter() {
        PolicyRequest.FilterSelection filterSelection = new PolicyRequest.FilterSelection();
        filterSelection.setType("Age");
        filterSelection.setEnabled(false);
        PolicyRequest.StrategySelection strategySelection = new PolicyRequest.StrategySelection();
        strategySelection.setStrategy("REDACT");
        filterSelection.setStrategies(Arrays.asList(strategySelection));

        PolicyRequest request = new PolicyRequest();
        request.setName("test-policy");
        request.setFilters(Arrays.asList(filterSelection));

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"enabled\": false");
    }

    @Test
    public void shouldGeneratePolicyWithIgnoredTerms() {
        PolicyRequest request = new PolicyRequest();
        request.setName("ignored-policy");
        request.setIgnored(Arrays.asList("term1", "term2"));

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"ignored\": [");
        assertThat(response.getBody()).contains("\"term1\"");
        assertThat(response.getBody()).contains("\"term2\"");
    }

    @Test
    public void shouldGeneratePolicyWithIgnoredPatterns() {
        PolicyRequest request = new PolicyRequest();
        request.setName("ignored-patterns-policy");
        request.setIgnoredPatterns(Arrays.asList("[0-9]{3}", "[A-Z]{2}"));

        ResponseEntity<String> response = restTemplate.postForEntity("/generate", request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"ignoredPatterns\": [");
        assertThat(response.getBody()).contains("\"pattern\": \"[0-9]{3}\"");
        assertThat(response.getBody()).contains("\"pattern\": \"[A-Z]{2}\"");
    }
}

package ai.philterd.policyeditor;

import java.util.ArrayList;
import java.util.List;

public class PolicyRequest {
    private String name;
    private List<FilterSelection> filters = new ArrayList<>();
    private PostFiltersSelection postFilters = new PostFiltersSelection();
    private PdfSelection pdf = new PdfSelection();
    private SplittingSelection splitting = new SplittingSelection();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FilterSelection> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterSelection> filters) {
        this.filters = filters;
    }

    public PostFiltersSelection getPostFilters() {
        return postFilters;
    }

    public void setPostFilters(PostFiltersSelection postFilters) {
        this.postFilters = postFilters;
    }

    public PdfSelection getPdf() {
        return pdf;
    }

    public void setPdf(PdfSelection pdf) {
        this.pdf = pdf;
    }

    public SplittingSelection getSplitting() {
        return splitting;
    }

    public void setSplitting(SplittingSelection splitting) {
        this.splitting = splitting;
    }

    public static class PostFiltersSelection {
        private boolean removeTrailingPeriods;
        private boolean removeTrailingSpaces;
        private boolean removeTrailingNewLines;

        public boolean isRemoveTrailingPeriods() {
            return removeTrailingPeriods;
        }

        public void setRemoveTrailingPeriods(boolean removeTrailingPeriods) {
            this.removeTrailingPeriods = removeTrailingPeriods;
        }

        public boolean isRemoveTrailingSpaces() {
            return removeTrailingSpaces;
        }

        public void setRemoveTrailingSpaces(boolean removeTrailingSpaces) {
            this.removeTrailingSpaces = removeTrailingSpaces;
        }

        public boolean isRemoveTrailingNewLines() {
            return removeTrailingNewLines;
        }

        public void setRemoveTrailingNewLines(boolean removeTrailingNewLines) {
            this.removeTrailingNewLines = removeTrailingNewLines;
        }
    }

    public static class PdfSelection {
        private String redactionColor = "black";
        private String replacementFont = "Helvetica";
        private boolean showReplacement = false;
        private float scale = 1.0f;
        private int dpi = 150;
        private float compressionQuality = 1.0f;
        private boolean preserveUnredactedPages = false;

        public String getRedactionColor() { return redactionColor; }
        public void setRedactionColor(String redactionColor) { this.redactionColor = redactionColor; }
        public String getReplacementFont() { return replacementFont; }
        public void setReplacementFont(String replacementFont) { this.replacementFont = replacementFont; }
        public boolean isShowReplacement() { return showReplacement; }
        public void setShowReplacement(boolean showReplacement) { this.showReplacement = showReplacement; }
        public float getScale() { return scale; }
        public void setScale(float scale) { this.scale = scale; }
        public int getDpi() { return dpi; }
        public void setDpi(int dpi) { this.dpi = dpi; }
        public float getCompressionQuality() { return compressionQuality; }
        public void setCompressionQuality(float compressionQuality) { this.compressionQuality = compressionQuality; }
        public boolean isPreserveUnredactedPages() { return preserveUnredactedPages; }
        public void setPreserveUnredactedPages(boolean preserveUnredactedPages) { this.preserveUnredactedPages = preserveUnredactedPages; }
    }

    public static class SplittingSelection {
        private boolean enabled = false;
        private String method = "newline";
        private int threshold = 1000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
    }

    public static class StrategySelection {
        private String strategy;
        private String condition;
        private Integer truncateDigits;
        private Integer truncateLeaveCharacters;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getTruncateDigits() {
            return truncateDigits;
        }

        public void setTruncateDigits(Integer truncateDigits) {
            this.truncateDigits = truncateDigits;
        }

        public Integer getTruncateLeaveCharacters() {
            return truncateLeaveCharacters;
        }

        public void setTruncateLeaveCharacters(Integer truncateLeaveCharacters) {
            this.truncateLeaveCharacters = truncateLeaveCharacters;
        }
    }

    public static class FilterSelection {
        private String type;
        private List<StrategySelection> strategies = new ArrayList<>();
        private boolean requireDelimiter;
        private boolean validate;

        // Common dynamic filter options
        private String sensitivity;
        private boolean capitalized;
        private boolean fuzzy;

        // EmailAddress options
        private boolean onlyStrictMatches;
        private boolean onlyValidTLDs;

        // CreditCard options
        private boolean onlyValidCreditCardNumbers;
        private boolean ignoreWhenInUnixTimestamp;
        private boolean onlyWordBoundaries;

        // Date options
        private boolean onlyValidDates;

        // IbanCode options
        private boolean onlyValidIBANCodes;
        private boolean allowSpaces;

        // TrackingNumber options
        private boolean ups;
        private boolean fedex;
        private boolean usps;

        // PhEye options
        private boolean removePunctuation;

        // PhEye specific configuration
        private String endpoint;
        private String bearerToken;
        private Integer timeout;
        private Integer maxIdleConnections;
        private List<String> labels;

        // Dictionary specific configuration
        private String classification;


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<StrategySelection> getStrategies() {
            return strategies;
        }

        public void setStrategies(List<StrategySelection> strategies) {
            this.strategies = strategies;
        }

        public boolean isRequireDelimiter() {
            return requireDelimiter;
        }

        public void setRequireDelimiter(boolean requireDelimiter) {
            this.requireDelimiter = requireDelimiter;
        }

        public boolean isValidate() {
            return validate;
        }

        public void setValidate(boolean validate) {
            this.validate = validate;
        }

        public String getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(String sensitivity) {
            this.sensitivity = sensitivity;
        }

        public boolean isCapitalized() {
            return capitalized;
        }

        public void setCapitalized(boolean capitalized) {
            this.capitalized = capitalized;
        }

        public boolean isFuzzy() {
            return fuzzy;
        }

        public void setFuzzy(boolean fuzzy) {
            this.fuzzy = fuzzy;
        }

        public boolean isOnlyStrictMatches() {
            return onlyStrictMatches;
        }

        public void setOnlyStrictMatches(boolean onlyStrictMatches) {
            this.onlyStrictMatches = onlyStrictMatches;
        }

        public boolean isOnlyValidTLDs() {
            return onlyValidTLDs;
        }

        public void setOnlyValidTLDs(boolean onlyValidTLDs) {
            this.onlyValidTLDs = onlyValidTLDs;
        }

        public boolean isOnlyValidCreditCardNumbers() {
            return onlyValidCreditCardNumbers;
        }

        public void setOnlyValidCreditCardNumbers(boolean onlyValidCreditCardNumbers) {
            this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        }

        public boolean isIgnoreWhenInUnixTimestamp() {
            return ignoreWhenInUnixTimestamp;
        }

        public void setIgnoreWhenInUnixTimestamp(boolean ignoreWhenInUnixTimestamp) {
            this.ignoreWhenInUnixTimestamp = ignoreWhenInUnixTimestamp;
        }

        public boolean isOnlyWordBoundaries() {
            return onlyWordBoundaries;
        }

        public void setOnlyWordBoundaries(boolean onlyWordBoundaries) {
            this.onlyWordBoundaries = onlyWordBoundaries;
        }

        public boolean isOnlyValidDates() {
            return onlyValidDates;
        }

        public void setOnlyValidDates(boolean onlyValidDates) {
            this.onlyValidDates = onlyValidDates;
        }

        public boolean isOnlyValidIBANCodes() {
            return onlyValidIBANCodes;
        }

        public void setOnlyValidIBANCodes(boolean onlyValidIBANCodes) {
            this.onlyValidIBANCodes = onlyValidIBANCodes;
        }

        public boolean isAllowSpaces() {
            return allowSpaces;
        }

        public void setAllowSpaces(boolean allowSpaces) {
            this.allowSpaces = allowSpaces;
        }

        public boolean isUps() {
            return ups;
        }

        public void setUps(boolean ups) {
            this.ups = ups;
        }

        public boolean isFedex() {
            return fedex;
        }

        public void setFedex(boolean fedex) {
            this.fedex = fedex;
        }

        public boolean isUsps() {
            return usps;
        }

        public void setUsps(boolean usps) {
            this.usps = usps;
        }

        public boolean isRemovePunctuation() {
            return removePunctuation;
        }

        public void setRemovePunctuation(boolean removePunctuation) {
            this.removePunctuation = removePunctuation;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBearerToken() {
            return bearerToken;
        }

        public void setBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Integer getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public void setMaxIdleConnections(Integer maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
        }

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }
    }
}

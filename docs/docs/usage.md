# Usage Guide

This guide describes how to use the Philterd Policy Editor to create and manage your redaction policies for Philter and Phileas.

## Creating a Policy

1. **Set Policy Name**: Provide a unique name for your policy.
2. **Add Filters**: Click "Add PII Filter" to start adding data types you wish to redact.
3. **Configure Strategies**: Each filter can have one or more strategies (e.g., REDACT, MASK, RANDOM_REPLACE).
    - **Redaction Format**: If using the `REDACT` strategy, you can specify a `redactionFormat` (e.g., `{{{REDACTED-%t}}}`).
    - **Mask Character**: If using the `MASK` strategy, you can specify a `maskCharacter` (e.g., `*`).
    - **Mask Length**: If using the `MASK` strategy, you can specify a `maskLength` (e.g., `5`) to force a specific length of the mask.
    - **Replacement Scope**: If using the `RANDOM_REPLACE` strategy, you can specify the `replacementScope` as `CONTEXT` or `DOCUMENT`.
    - **Truncate Length**: For `Zip Code` filters using the `TRUNCATE` strategy, you can specify the `truncateLength` (1, 2, 3, or 4).
4. **Conditions**: Optionally add conditions like `confidence > 0.8` to strategies.

## Filter Types

The editor supports a wide range of filters:
- **Common Filters**: Age, Credit Card, Email, SSN, etc.
- **Location Filters**: City, County, State, Zip Code.
- **AI/NER Filters**: PhEye (for ML-based detection).
- **Custom Filters**: Dictionary (upload your own terms).

## Advanced Options

Click the **Advanced Options** button to reveal additional settings:
- **Post Filters**: Remove trailing periods, spaces, or newlines from redacted text.
- **PDF Settings**: Configure redaction color, font types, and DPI for PDF processing.
- **Splitting**: Define how documents should be split (e.g., by newline) during processing.

## Authoring with PhiSQL

A policy can be written in [PhiSQL](https://github.com/philterd/phisql), a declarative language for
PII operations, instead of built up through the form:

1. **Author with PhiSQL**: Click the "Author with PhiSQL" button to open the authoring panel.
2. **Write the policy**: For example:

    ```sql
    POLICY ssn_only;

    REDACT SSN WITH MASK;
    ```

3. **Compile to Policy**: The PhiSQL is compiled to a redaction policy, validated against the policy
   schema, and loaded into the form below, where it can be edited, tested, and downloaded like any
   other policy. Compiling replaces what is currently in the form.

Parse and compile errors are listed under the panel with the line and column they occurred at. The
compiled policy is rejected if it does not conform to the schema, so a policy that loads into the
form is one the editor could have produced itself.

## Testing Policies

Once you have generated a policy, you can test it directly within the editor:

1. **Generate Policy**: Click the "Generate Policy" button.
2. **Test Policy**: Click the "Test Policy" button that appears in the results section.
3. **Input Text**: Enter the text you want to redact into the "Input Text" textarea, or pick an entry from **Load Sample Text** to fill it with a bundled synthetic document (a clinical note, a financial record, a legal filing, or a support email). The sample text is fabricated and contains no real person's data, so a policy can be tried without supplying anything of your own.
4. **Redact**: Click the "Redact" button.
5. **Review Results** in the two output tabs:
    - **Redacted Text**: The output with redactions applied according to your policy. Each redacted span is highlighted, and hovering over a highlight shows the filter that matched and its confidence. A value the policy detected but left in the clear is highlighted differently, which is what to look for when a value was not redacted.
    - **Explanation**: The full JSON explanation, listing every span that was applied and every span that was identified.

Testing runs the Phileas engine bundled with the editor, so it needs no Philter deployment and no network access. Filters backed by an AI model, such as PhEye, cannot be tested here, because those require a running PhEye service.

Detection is probabilistic. Review the highlighted output against your own data before relying on a policy in production.

## Exporting and Importing

- **Generate Policy**: Click this to see the JSON representation of your current configuration.
- **Copy to Clipboard**: Quickly copy the JSON for use in your application.
- **Download Policy**: Save the policy as a `.json` file.
- **Load Preset**: Quickly populate the editor with pre-configured filters for specific domains. Presets are intended to be built upon and customized for your use. They are not intended to be comprehensive policies for any industry. Review them carefully and thoroughly before use.
    - **Legal**: Includes filters for common legal entities like names, cities, states, street addresses, and phone numbers.
    - **Financial**: Focuses on financial identifiers such as credit card numbers, IBAN codes, bank routing numbers, and bitcoin addresses.
    - **Healthcare**: Designed for healthcare data, including patient names, dates, SSNs, phone numbers, zip codes, and email addresses.
- **Upload Policy**: Choose an existing Philter or Phileas JSON policy to load it back into the editor for further modification.
- **Reset Editor**: Clear all fields and start over.

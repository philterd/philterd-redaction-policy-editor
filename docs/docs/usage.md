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
    - **Anonymization Candidates**: If using the `RANDOM_REPLACE` strategy, you can provide a list of candidates (one per line) that Phileas will choose from to replace the PII.
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

## Testing Policies

Once you have generated a policy, you can test it directly within the editor:

1. **Generate Policy**: Click the "Generate Policy" button.
2. **Test Policy**: Click the "Test Policy" button that appears in the results section.
3. **Input Text**: Paste the text you want to redact into the "Text to Redact" textarea.
4. **Redact**: Click the "Redact" button.
5. **Review Results**:
    - **Redacted Text**: View the output with redactions applied according to your policy.
    - **Show Explanation**: Click this button to see a detailed JSON explanation of why specific terms were redacted and which filters were triggered.

## Exporting and Importing

- **Generate Policy**: Click this to see the JSON representation of your current configuration.
- **Copy to Clipboard**: Quickly copy the JSON for use in your application.
- **Download Policy**: Save the policy as a `.json` file.
- **Upload Policy**: Choose an existing Philter or Phileas JSON policy to load it back into the editor for further modification.
- **Reset Editor**: Clear all fields and start over.

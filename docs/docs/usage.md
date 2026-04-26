# Usage Guide

This guide describes how to use the Philterd Policy Editor to create and manage your redaction policies for Philter and Phileas.

## Creating a Policy

1. **Set Policy Name**: Provide a unique name for your policy.
2. **Add Filters**: Click "Add PII Filter" to start adding data types you wish to redact.
3. **Configure Strategies**: Each filter can have one or more strategies (e.g., REDACT, MASK, RANDOM_REPLACE).
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

## Exporting and Importing

- **Generate Policy**: Click this to see the JSON representation of your current configuration.
- **Copy to Clipboard**: Quickly copy the JSON for use in your application.
- **Download Policy**: Save the policy as a `.json` file.
- **Upload Policy**: Choose an existing Philter or Phileas JSON policy to load it back into the editor for further modification.
- **Reset Editor**: Clear all fields and start over.

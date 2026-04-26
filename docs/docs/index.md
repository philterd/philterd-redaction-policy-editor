# Philterd Policy Editor

Welcome to the Philterd Policy Editor documentation. This tool provides a user-friendly interface for building and managing redaction policies for [Philter](https://www.philterd.ai) and [Phileas](https://github.com/philterd/phileas), the open-source PII/PHI redaction engine.

## Features

- **Dynamic Filter Selection**: Choose from over 30 PII/PHI filter types.
- **Multiple Strategies**: Configure multiple redaction strategies per filter with optional conditions.
- **Advanced Configuration**: Fine-tune PDF redaction settings, document splitting, and post-filtering.
- **Policy Management**: Upload existing JSON policies to edit, and download or copy generated policies.
- **Docker Support**: Easy deployment using Docker and Docker Compose.

## Redaction Policies

Philter and Phileas use JSON-based policies to define how sensitive information should be identified and redacted. This editor simplifies the creation of these complex JSON structures by providing a graphical interface that maps directly to the policy model used by both Philter and Phileas.

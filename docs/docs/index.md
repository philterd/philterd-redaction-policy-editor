# Philterd Policy Editor

![Philterd Policy Editor](screenshot.png)

Welcome to the Philterd Policy Editor documentation. This tool provides a user-friendly interface for building and managing redaction policies for [Philter](https://www.philterd.ai) and [Phileas](https://github.com/philterd/phileas), the open-source PII/PHI redaction engine.

## Features

- **Dynamic Filter Selection**: Choose from over 30 PII/PHI filter types.
- **Multiple Strategies**: Configure multiple redaction strategies per filter with optional conditions.
- **Advanced Configuration**: Fine-tune PDF redaction settings, document splitting, and post-filtering.
- **Policy Management**: Upload existing JSON policies to edit, and download or copy generated policies.
- **Policy Testing**: Run a policy against text in the browser and see which spans were redacted, with
  bundled synthetic sample documents so no real data is needed.
- **Author with PhiSQL**: Write a policy in PhiSQL and compile it into the editor.
- **Health and Metrics**: `/api/health` reports the status and application version, and
  `/actuator/health` and `/actuator/prometheus` cover probing and scraping.
- **Docker Support**: Easy deployment using Docker and Docker Compose.

## Redaction Policies

Philter and Phileas use JSON-based policies to define how sensitive information should be identified and redacted. This editor simplifies the creation of these complex JSON structures by providing a graphical interface that maps directly to the policy model used by both Philter and Phileas.

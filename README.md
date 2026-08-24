# Philterd Policy Editor

The Philterd Policy Editor provides a user-friendly web interface for building and managing redaction policies
for [Philter](https://www.github.com/philterd/philter) and [Phileas](https://github.com/philterd/phileas).

Documentation is available at https://philterd.github.io/philterd-redaction-policy-editor/

![Philterd Policy Editor](docs/docs/screenshot.png)

## Features

- **Schema-Driven**: The entire form is generated from the official redaction policy
  [JSON Schema](https://github.com/philterd/phisql/tree/main/schema). Generated policies are
  validated against the schema, so they always conform.
- **Multiple Schema Versions**: Select which policy schema version to author. The editor bundles
  schema versions 1.0.0, 1.1.0, and 1.2.0, one folder per version under
  `src/main/resources/schemas/<version>/`, and defaults to the newest. Adding a new version is a
  matter of dropping in its schema file, with no code changes required.
- **Dynamic Filter Selection**: Choose from over 30 PII/PHI filter types.
- **Multiple Strategies**: Configure multiple redaction strategies per filter with optional conditions.
- **Advanced Configuration**: Fine-tune PDF redaction settings, document splitting, and post-filtering.
- **Policy Management**: Load presets for common use cases (Legal, Financial, Healthcare), upload
  existing JSON policies to edit, and download or copy generated policies.
- **Policy Testing**: Test your policies against sample text directly in the browser and view detailed
  redaction explanations. Testing runs the bundled Phileas engine, so it is available for the schema
  version that engine supports (see `phileas.supported-schema-version`).
- **Docker Support**: Easy deployment using Docker and Docker Compose.

## Configuration

The Philterd Policy Editor can be configured using environment variables:

| Environment Variable | Description                                                                         | Default |
|----------------------|-------------------------------------------------------------------------------------|---------|
| `HIDE_PII_WARNING`   | Set to `1` to hide the PII warning banner.                                          | `0`     |
| `CUSTOM_HEADER_FILE` | Path to a file containing custom HTML to be inserted into the `<head>` of the page. | (empty) |
| `CUSTOM_FOOTER_FILE` | Path to a file containing custom HTML to be inserted into the footer of the page.   | (empty) |

The schema version that the **Test Policy** feature can run is controlled by the
`phileas.supported-schema-version` application property (overridable with the
`PHILEAS_SUPPORTEDSCHEMAVERSION` environment variable). It must match the schema version supported by
the bundled Phileas runtime. Authoring and downloading policies works for every bundled schema
version regardless of this setting.

## Schema versions

The editor bundles the redaction policy schemas copied verbatim from
[philterd/phisql](https://github.com/philterd/phisql/tree/main/schema) and authors against the newest
by default. Which version each bundled dependency uses:

| Bundled dependency | Version | Redaction policy schema |
|--------------------|---------|-------------------------|
| PhiSQL (Author with PhiSQL) | 1.3.0 | 1.2.0 |
| Phileas (Test Policy) | 4.2.0 | 1.1.0 |

Each Phileas release understands exactly one schema version, so **Test Policy** is available for
schema 1.1.0 only. Policies compiled from PhiSQL target schema 1.2.0 and can be authored, validated,
and downloaded, but cannot be tested in the browser until a Phileas release that supports 1.2.0 is
bundled.

## Getting Started

### Using Docker

You can pull the Docker image directly from DockerHub:

```bash
docker run -p 8080:8080 philterd/philterd-redaction-policy-editor:latest
```

Alternatively, you can use `docker-compose`:

```bash
docker-compose build
docker-compose up
```

Either way, you can now access the editor at `http://localhost:8080`.

## License

Distributed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2026 [Philterd, LLC](https://www.philterd.ai)

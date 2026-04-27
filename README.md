# Philterd Policy Editor

The Philterd Policy Editor provides a user-friendly web interface for building and managing redaction policies for [Philter](https://www.github.com/philterd/philter) and [Phileas](https://github.com/philterd/phileas).

Documentation is available at https://philterd.github.io/philterd-redaction-policy-editor/

![Philterd Policy Editor](docs/docs/screenshot.png)

## Features

- **Dynamic Filter Selection**: Choose from over 30 PII/PHI filter types.
- **Multiple Strategies**: Configure multiple redaction strategies per filter with optional conditions.
- **Advanced Configuration**: Fine-tune PDF redaction settings, document splitting, and post-filtering.
- **Policy Management**: Load presets for common use cases (Legal, Financial), upload existing JSON policies to edit, and download or copy generated policies.
- **Policy Testing**: Test your policies against sample text directly in the browser and view detailed redaction explanations.
- **Docker Support**: Easy deployment using Docker and Docker Compose.

## Configuration

The Philterd Policy Editor can be configured using environment variables:

| Environment Variable | Description | Default |
| --- | --- | --- |
| `HIDE_PII_WARNING` | Set to `1` to hide the PII warning banner. | `0` |
| `GOOGLE_ANALYTICS_TRACKING_ID` | The Google Analytics tracking ID. | |

## Getting Started

### Using Docker

```bash
docker-compose build
docker-compose up
```

### Using Makefile

The project includes a `Makefile` for common development tasks:

```bash
# Build the JAR and the Docker image
make

# Build just the JAR
make build-jar

# Build just the Docker image
make build-docker

# Push the Docker image to DockerHub
make push-docker

# Run tests
make test

# Clean build artifacts
make clean
```

You can now access the editor at `http://localhost:8080`. To access it from other machines on your network, use the host's IP address: `http://<host-ip>:8080`.

## License

Distributed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2026 [Philterd, LLC](https://www.philterd.ai)

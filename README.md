# Philterd Policy Editor

The Philterd Policy Editor provides a user-friendly web interface for building and managing redaction policies for [Philter](https://www.philterd.ai) and [Phileas](https://github.com/philterd/phileas).

## Features

- **Dynamic Filter Selection**: Choose from over 30 PII/PHI filter types.
- **Multiple Strategies**: Configure multiple redaction strategies per filter with optional conditions.
- **Advanced Configuration**: Fine-tune PDF redaction settings, document splitting, and post-filtering.
- **Policy Management**: Upload existing JSON policies to edit, and download or copy generated policies.
- **Docker Support**: Easy deployment using Docker and Docker Compose.

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose** (recommended)
- OR **Java 17** and **Maven 3.x** (for local builds)

### Running with Docker

1. Clone the repository.
2. Run the following command in the project root:

```bash
docker-compose up --build
```

3. Access the editor at `http://localhost:8080`.

### Building and Running Locally

1. Package the application:

```bash
mvn clean package
```

2. Run the generated JAR file:

```bash
java -jar target/policy-editor-0.0.1-SNAPSHOT.jar
```

3. Access the editor at `http://localhost:8080`.

## Documentation

Full documentation is available in the `docs/docs` directory or can be viewed via [MkDocs](https://www.mkdocs.org/):

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

## License

Distributed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2026 [Philterd, LLC](https://www.philterd.ai)

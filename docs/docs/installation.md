# Installation

The Philterd Policy Editor can be run using Docker or built locally using Maven.

## Prerequisites

- **Docker**: (Optional) For running via containers.
- **Java 17**: For local builds.
- **Maven 3.x**: For local builds.

## Running with Docker

The easiest way to get started is using Docker Compose:

1. Clone the repository.
2. Run the following command in the project root:

```bash
docker-compose up --build
```

3. Access the editor at `http://localhost:8080`.

## Building and Running Locally

To build the project from source:

1. Package the application:

```bash
mvn clean package
```

2. Run the generated JAR file:

```bash
java -jar target/philterd-policy-editor.jar
```

3. Access the editor at `http://localhost:8080`.

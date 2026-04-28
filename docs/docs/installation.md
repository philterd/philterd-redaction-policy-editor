# Installation

The Philterd Policy Editor can be run using Docker or built locally using Maven.

## Prerequisites

- **Docker**: (Optional) For running via containers.
- **Java 21**: For local builds.
- **Maven 3.x**: For local builds.

## Running with Docker

The easiest way to get started is to pull the image from DockerHub:

```bash
docker pull philterd/philterd-redaction-policy-editor:latest
docker run -p 8080:8080 philterd/philterd-redaction-policy-editor:latest
```

### Using Docker Compose

Alternatively, if you have cloned the repository, you can use Docker Compose:

1. Run the following command in the project root:

```bash
docker-compose up
```

2. Access the editor at `http://localhost:8080`.
4. To allow others on your network to access the editor, ensure your firewall allows traffic on port `8080` and use your machine's IP address (e.g., `http://192.168.1.10:8080`).

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

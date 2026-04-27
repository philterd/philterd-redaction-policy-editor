# Installation

The Philterd Policy Editor can be run using Docker or built locally using Maven.

## Prerequisites

- **Docker**: (Optional) For running via containers.
- **Java 21**: For local builds.
- **Maven 3.x**: For local builds.

## Running with Docker

The easiest way to get started is using Docker Compose:

1. Clone the repository.
2. Run the following command in the project root:

```bash
docker-compose up --build
```

3. Access the editor at `http://localhost:8080`.
4. To allow others on your network to access the editor, ensure your firewall allows traffic on port `8080` and use your machine's IP address (e.g., `http://192.168.1.10:8080`).

## Configuration

The Philterd Policy Editor can be configured using environment variables:

| Environment Variable | Description | Default |
| --- | --- | --- |
| `HIDE_PII_WARNING` | Set to `1` to hide the PII warning banner. | `0` |
| `GOOGLE_ANALYTICS_TRACKING_ID` | The Google Analytics tracking ID. | |

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
4. To allow others on your network to access the editor, ensure your firewall allows traffic on port `8080` and use your machine's IP address (e.g., `http://192.168.1.10:8080`).

## Network Configuration

By default, the Philterd Policy Editor listens on all network interfaces (`0.0.0.0`) on port `8080`.

### Running on a Network

If you are running the editor on a server or a machine within your own network and want to access it from another device:

1. **Identify the Host IP Address**: Find the IP address of the machine running the editor.
2. **Access via IP**: Instead of `localhost`, use the IP address in your browser: `http://<host-ip-address>:8080`.
3. **Firewall Rules**: Ensure that port `8080` is open in the host machine's firewall to allow incoming connections.

### Docker Port Mapping

When running with Docker, the port mapping in `docker-compose.yml` (or the `-p` flag in `docker run`) determines how the editor is exposed to the network. The default configuration:

```yaml
ports:
  - "8080:8080"
```

This maps port `8080` on the host to port `8080` in the container, making it accessible to the network via the host's IP.

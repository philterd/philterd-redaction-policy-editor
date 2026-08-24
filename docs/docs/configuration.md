# Configuration

The Philterd Policy Editor can be configured using environment variables:

| Environment Variable | Description | Default |
| --- | --- | --- |
| `HIDE_PII_WARNING` | Set to `1` to hide the PII warning banner. | `0` |
| `CUSTOM_HEADER_FILE` | Path to a file containing custom HTML to be inserted into the `<head>` of the page. | (empty) |
| `CUSTOM_FOOTER_FILE` | Path to a file containing custom HTML to be inserted into the footer of the page. | (empty) |

## Policy schema version

The editor authors a single redaction policy schema version: the one the bundled Phileas runtime can
run, so any policy built here can also be tested here. The version is shown on the page next to the
application version and commit, and there is nothing to configure. Moving to a newer schema means
upgrading the editor to a build whose Phileas, PhiSQL, and bundled schema all target it.

## Health and metrics

Two Spring Boot Actuator endpoints are exposed for monitoring, matching Philter:

| Endpoint | Description |
| --- | --- |
| `/actuator/health` | Returns HTTP 200 with a body containing `"status":"UP"` while the application is serving. |
| `/actuator/prometheus` | JVM, process, Tomcat session, and HTTP request metrics in Prometheus text format. |

Both are reachable without authentication. Point a monitoring system at `/actuator/health` and a
Prometheus scrape job at `/actuator/prometheus`:

```yaml
scrape_configs:
  - job_name: policy-editor
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["localhost:8080"]
```

The editor registers no metrics of its own, so what is exported is the standard Spring Boot set:
heap and garbage collection, thread and class counts, Tomcat session counts, process uptime and CPU,
disk space, and `http_server_requests_seconds` timings broken down by URI and status.

Kubernetes-style probes are available as sub-paths of the health endpoint, `/actuator/health/liveness`
and `/actuator/health/readiness`. Apart from those, no other actuator endpoint responds. Two
application properties control what is published:

| Property | Value | Description |
| --- | --- | --- |
| `management.endpoints.web.exposure.include` | `health,prometheus` | The only endpoints published. Everything else returns 404. |
| `management.endpoints.web.discovery.enabled` | `false` | Turns off the discovery index at `/actuator`, which would otherwise list the published endpoints. |

Either can be overridden at runtime with the matching environment variable, for example
`MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`.

The `docker-compose.yml` in the repository uses `/actuator/health` as the container healthcheck, so
`docker compose ps` reports the service healthy only once it answers with an `UP` status. The runtime
image contains no `curl` or `wget`, so the probe speaks HTTP over bash's `/dev/tcp`.

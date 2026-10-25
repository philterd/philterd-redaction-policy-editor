# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - TBD

### Added
- Added `CUSTOM_HEADER_FILE` environment variable to allow inserting custom HTML into the page header.
- Added `CUSTOM_FOOTER_FILE` environment variable to allow inserting custom HTML into the page footer.
- Updated `docker-compose.yml` with examples for using `CUSTOM_HEADER_FILE` and `CUSTOM_FOOTER_FILE` with mounts.
- Added a "Fork on GitHub" badge to the index page.
- Added version number on page, alongside the policy schema version the editor authors.
- Added redaction policy schema version 1.3.0.
- Added Spring Boot Actuator with `/actuator/health` and `/actuator/prometheus`. Only those two
  endpoints are exposed, and the discovery index at `/actuator` is disabled.
- Added a `/api/health` endpoint that reports `{"status": "UP", "applicationVersion": "<version>"}`,
  matching the health contract shared across Philterd products. The version comes from the build.
- Added a container healthcheck to `docker-compose.yml` that requires an `UP` health status.
- The Test Policy output is now two tabs: the redacted text, with each redacted span highlighted, and
  the explanation. Values that were detected but left in the clear are highlighted differently.
- Added a "Load Sample Text" menu to the Test Policy input, offering four synthetic documents so a
  policy can be tried without entering real data.
- Added "Copy PhiSQL" and "Download PhiSQL", which write the current policy out as PhiSQL. The output
  is verified before it is offered: the editor compiles what it generated and compares the result to
  the policy, and reports which part of the policy has no PhiSQL form rather than exporting source
  that means something else. The existing JSON actions are now labelled "Copy JSON" and
  "Download JSON".

### Changed
- The editor now authors a single redaction policy schema version, the one the bundled Phileas
  runtime can run, so every policy it produces can be tested in the browser. The policy schema
  version dropdown has been removed, and the version is shown on the page instead. The application
  fails to start unless exactly one schema is bundled.
- Upgraded Phileas to 4.5.0 and PhiSQL to 1.6.0. Both work with redaction policy schema 1.3.0, which
  replaces the previously bundled 1.0.0.
- Removed `physicianName` from the healthcare preset. Schema 1.3.0 removed the filter; physician names
  are now detected through PhEye.
- Schema 1.3.0 renamed `zipCodeFilterStrategy` to `zipCodeFilterStrategies`. New zip code filters use
  the new name, and a policy that still uses the old one exports to PhiSQL.
- The bundled schema is copied verbatim from `philterd/phisql`: the crypto settings no longer require
  an `iv`, because `CRYPTO_REPLACE` generates a per-value AES-GCM nonce.
- Building with Java 25, which the released PhiSQL artifacts require.

### Removed
- Removed the policy schema version selector and the `?version=` URL parameter.

### Fixed
- A newly added filter strategy now carries the strategy shown in its dropdown. The control reported
  no value until it was changed, so a filter left on the default serialized without a strategy and
  disappeared from the generated policy.
- `/api/compile` now fails with an error when the schema version PhiSQL targets is not bundled,
  instead of returning a policy that was never validated.
- Testing a policy that uses the PhEye filter now explains that model-backed detection runs in a
  separate service and cannot be tested in the editor, instead of surfacing an internal error.
- Text submitted for testing, and PhiSQL source submitted for compiling, no longer reach the
  application log on the error path. Failures are logged by exception type and origin.
- The JavaScript tests are skipped when tests are skipped, so `mvn package -DskipTests` no longer
  requires Node. The Docker build uses that command.

## 1.0.0 - 2026-04-27

- Initial release of the Philterd Redaction Policy Editor.

### Added
- Support for generating Philter redaction policies.
- Support for PhEye and Dictionaries in policies.
- Support for PDF redaction and splitting options.

### Changed
- Upgraded to Spring Boot 4.0.6 and fixed related test issues.

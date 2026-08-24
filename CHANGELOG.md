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
- Added version number on page.
- Added the bundled PhiSQL version to the version line on the page.
- Added redaction policy schema versions 1.1.0 and 1.2.0. The version selector offers 1.0.0, 1.1.0,
  and 1.2.0 and defaults to the newest.

### Changed
- Upgraded PhiSQL to 1.3.0, which compiles to redaction policy schema 1.2.0.
- Upgraded Phileas to 4.2.0 and set `phileas.supported-schema-version` to 1.1.0, so **Test Policy**
  now runs against schema 1.1.0.
- Re-synced the bundled 1.0.0 schema with the canonical schema in `philterd/phisql`: the crypto
  settings no longer require an `iv`, because `CRYPTO_REPLACE` generates a per-value AES-GCM nonce.
- Building with Java 25, which the released PhiSQL artifacts require.

### Fixed
- `/api/compile` now fails with an error when the schema version PhiSQL targets is not bundled,
  instead of returning a policy that was never validated.

## 1.0.0 - 2026-04-27

- Initial release of the Philterd Redaction Policy Editor.

### Added
- Support for generating Philter redaction policies.
- Support for PhEye and Dictionaries in policies.
- Support for PDF redaction and splitting options.

### Changed
- Upgraded to Spring Boot 4.0.6 and fixed related test issues.

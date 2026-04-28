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

## 1.0.0 - 2026-04-27

- Initial release of the Philterd Redaction Policy Editor.

### Added
- Support for generating Philter redaction policies.
- Support for PhEye and Dictionaries in policies.
- Support for PDF redaction and splitting options.

### Changed
- Upgraded to Spring Boot 4.0.6 and fixed related test issues.

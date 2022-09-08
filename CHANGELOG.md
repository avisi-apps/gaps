# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Added reitit-express library `com.avisi-apps.gapsreitit-express`
- log modules is now also usable client side

### Changed
- log module now uses `pino` instead of `bunyan`

### Fixed

## [v0.0.48]
### Fixed
- Fix macro require of logging library for rcf preload

## [v0.0.45]
### Changed
- Change namespaces for rcf and log module to be correct with jar namespace. Changed from `avisi-apps.gaps` to 
`com.avisi-apps.gaps`

## [v0.0.41]
### Added
- Adds a Make command to trigger a release and update the changelog

### Fixed
- Fixed releasing by pushing a tag

## [0.0.30] - 2022-08-18
*Was still fighting with automation the release process here don't use this version*
### Added
- Added fulcro-google-remote library `com.avisi-apps.gaps/fulcro-google-remote-library
- Added clj-kondo linting
- Automated builds and release process

## [0.0.4] - 2022-08-11
### Added
- Added SCM config to published jars
- Added documentation to `com.avisi-apps.gaps/log`

## [0.0.1] - 2022-08-11
### Added
- Added log library `com.avisi-apps.gaps/log`
- Added rcf dev library `com.avisi-apps.gaps/log`
- Added build script to build or release all projects

[Unreleased]: https://github.com/avisi-apps/gaps/compare/v0.0.48...HEAD
[0.0.48]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.48
[0.0.45]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.45
[0.0.41]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.41
[0.0.30]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.30
[0.0.4]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.4
[0.0.1]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.1

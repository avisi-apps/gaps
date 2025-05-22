# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added

### Changed

### Fixed

## [v0.0.126]
### Added

### Changed
- Changed logging library from pino to telemere

### Fixed

## [v0.0.123]
### Added

### Changed

### Fixed

## [v0.0.120]
### Added

### Changed

### Fixed

## [v0.0.117]
### Added

### Changed

### Fixed

## [v0.0.114]
### Added

### Changed

### Fixed

## [v0.0.109]
### Added

### Changed

### Fixed

## [v0.0.106]
### Added

### Changed

### Fixed

## [v0.0.103]
### Added

### Changed

### Fixed

## [v0.0.100]
### Added

### Changed

### Fixed

## [v0.0.97]
### Added
- Add function to Brevo module that retrieves a contact's app attributes for email or brevo-id
### Changed

### Fixed

## [v0.0.94]
### Added
- Add README for Brevo module and include it in the top-level README
- Add function to Brevo module that returns an app attribute string as a map
- Add function to Brevo module that retrieves a contact's email address by their Brevo contact id
- Add function to Brevo module that retrieves a contact's app attributes

### Changed

### Fixed

## [v0.0.89]
### Added

### Changed

### Fixed

## [v0.0.77]
### Added
- Add Brevo field for monday monetization enabled status

### Changed

### Fixed

## [v0.0.74]
### Added

### Changed

### Fixed
- Fix namespaced classes not working as direct children in error boundary by explicitly binding comp/*parent* 
  to the parent retrieved from the error-boundary macro

## [v0.0.71]
### Added
- Added a log preload which enables pretty printing with `pino-pretty`
- Added a run configuration for the cloud run example

### Changed
- The log module now only uses the `:message` key in the json logging object we used to also write the message to  a `:msg`
  key to make it compatible with pino but we now fixed it by configuring the correct key. This saves us logging every 
  message twice.
- Improved error logging. It now won't log the exception-date als cljs object anymore which cause a lot of unecessary
  logging. We now make use of the default error formatter but only make sure that we transform ex-data to a js object.

### Fixed

## [v0.0.67]
### Added
- New Brevo module

### Changed

### Fixed

## [v0.0.64]
### Added
- New Rollbar module

### Changed
- Pino logger now instantiated by `defonce` instead of `def`

### Fixed

## [v0.0.61]
### Added

### Changed

### Fixed
- Request body gets converted to a bean before logging

## [v0.0.58]
### Added
- Added express body parsers (json, raw, text, urlencoded)
- Raw body parser now also accepts application/transit+json

### Changed

### Fixed

## [v0.0.55]
Broken release. Do not use.

## [v0.0.54]
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

[Unreleased]: https://github.com/avisi-apps/gaps/compare/v0.0.126...HEAD
[0.0.126]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.126
[0.0.123]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.123
[0.0.120]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.120
[0.0.117]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.117
[0.0.114]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.114
[0.0.109]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.109
[0.0.106]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.106
[0.0.103]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.103
[0.0.100]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.100
[0.0.97]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.97
[0.0.94]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.94
[0.0.89]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.89
[0.0.77]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.77
[0.0.74]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.74
[0.0.71]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.71
[0.0.67]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.67
[0.0.64]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.64
[0.0.61]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.61
[0.0.58]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.58
[0.0.54]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.54
[0.0.48]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.48
[0.0.45]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.45
[0.0.41]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.41
[0.0.30]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.30
[0.0.4]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.4
[0.0.1]: https://github.com/avisi-apps/gaps/releases/tag/v0.0.1

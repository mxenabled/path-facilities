# Changelog

## [2.0.0](https://github.com/mxenabled/path-facilities/compare/v1.5.2...v2.0.0) (2023-03-06)


### ⚠ BREAKING CHANGES

* All facility configs will need to be converted to durations instead of millis Fixing tests

### Features

* Change configuration millis to durations ([ad0d56e](https://github.com/mxenabled/path-facilities/commit/ad0d56edbac2a6899945875860d13439040a47c0))

## [1.5.2](https://github.com/mxenabled/path-facilities/compare/v1.5.1...v1.5.2) (2023-02-07)


### Bug Fixes

* raise path exceptions without wrapping in FaultTolerantExecutor ([cfca147](https://github.com/mxenabled/path-facilities/commit/cfca1477f3af299cff6dde927fef940b2ac1ab88))

## [1.5.1](https://github.com/mxenabled/path-facilities/compare/v1.5.0...v1.5.1) (2023-01-24)


### Bug Fixes

* update dependencies and set upper bound for core ([8d69bae](https://github.com/mxenabled/path-facilities/commit/8d69bae0cfae1db65c35848534d99ae14989efdb))

## [1.5.0](https://github.com/mxenabled/path-facilities/compare/v1.5.0-SNAPSHOT...v1.5.0) (2023-01-24)


### Miscellaneous Chores

* release 1.5.0 ([86b9d6c](https://github.com/mxenabled/path-facilities/commit/86b9d6c07cdc69e227da90a8cc05532d5b0e039d))

## [1.5.0-SNAPSHOT](https://github.com/mxenabled/path-facilities/compare/v1.4.0-SNAPSHOT...v1.5.0-SNAPSHOT) (2023-01-23)


### Features

* add honeybadger exception reporter facility ([cab99cd](https://github.com/mxenabled/path-facilities/commit/cab99cd38ec20bf99dbf109aec3cc4577c9ad443))


### Miscellaneous Chores

* release 1.4.0-SNAPSHOT ([f7355df](https://github.com/mxenabled/path-facilities/commit/f7355df3d6dfdcf0eb96f82187d1203da69a27c4))
* release 1.5.0-SNAPSHOT ([0f7bbdf](https://github.com/mxenabled/path-facilities/commit/0f7bbdf72b991725a134a7707a18129d072f0826))

## [1.4.0-SNAPSHOT](https://github.com/mxenabled/path-facilities/compare/v1.4.0-SNAPSHOT...v1.4.0-SNAPSHOT) (2023-01-23)


### Features

* add honeybadger exception reporter facility ([cab99cd](https://github.com/mxenabled/path-facilities/commit/cab99cd38ec20bf99dbf109aec3cc4577c9ad443))


### Miscellaneous Chores

* release 1.4.0-SNAPSHOT ([f7355df](https://github.com/mxenabled/path-facilities/commit/f7355df3d6dfdcf0eb96f82187d1203da69a27c4))

## [1.4.0-SNAPSHOT](https://github.com/mxenabled/path-facilities/compare/v1.3.2-SNAPSHOT...v1.4.0-SNAPSHOT) (2023-01-12)


### Miscellaneous Chores

* release 1.4.0-SNAPSHOT ([0871507](https://github.com/mxenabled/path-facilities/commit/0871507b178fced504d26a20fd794470a2aa373d))

## [1.3.2-SNAPSHOT](https://github.com/mxenabled/path-facilities/compare/v1.3.2...v1.3.2-SNAPSHOT) (2023-01-10)


### Features

* improve redis store configuration ([895f201](https://github.com/mxenabled/path-facilities/commit/895f201cb48adfbfcfbe574039b80f03af6da258))
* use configuration binding in encryption-service-vault ([61fd189](https://github.com/mxenabled/path-facilities/commit/61fd189e3ebf2fca6b5ada7dfaddd9fe5bed9721))
* use configuration binding in jasypt-encryption-service ([fb9e23c](https://github.com/mxenabled/path-facilities/commit/fb9e23ca48739c1e80fdb345529bbd2d3f98855f))
* use configuration binding in message-broker-nats ([8932273](https://github.com/mxenabled/path-facilities/commit/89322738d687647104603376f52450fdf42a4bfd))
* use configuration binding in vault-store ([eb461c1](https://github.com/mxenabled/path-facilities/commit/eb461c16fff31436e9718fad29644c084c702429))
* use Duration for encryption-service-vault retryInterval ([61fd189](https://github.com/mxenabled/path-facilities/commit/61fd189e3ebf2fca6b5ada7dfaddd9fe5bed9721))
* use duration for store-vault retryInterval ([eb461c1](https://github.com/mxenabled/path-facilities/commit/eb461c16fff31436e9718fad29644c084c702429))


### Bug Fixes

* add core dependency constraints to exclude bad core version ([3de69be](https://github.com/mxenabled/path-facilities/commit/3de69be0461f83c3c3bd09f624f4daad6b8e816b))
* change encryption-service-vault default authentication to APPROLE ([61fd189](https://github.com/mxenabled/path-facilities/commit/61fd189e3ebf2fca6b5ada7dfaddd9fe5bed9721))
* change store-vault default authentication to APPROLE ([3b69af9](https://github.com/mxenabled/path-facilities/commit/3b69af93f4943156e0e3206ff219ce840b7f244d))
* deprecate encryption-service-vault APPID authentication method ([61fd189](https://github.com/mxenabled/path-facilities/commit/61fd189e3ebf2fca6b5ada7dfaddd9fe5bed9721))
* deprecate store-vault APPID authentication method ([3b69af9](https://github.com/mxenabled/path-facilities/commit/3b69af93f4943156e0e3206ff219ce840b7f244d))
* utilize new configuration binding features ([a5ca597](https://github.com/mxenabled/path-facilities/commit/a5ca5979b7b0d411c235ddaf86fa922cb66e1379))


### Reverts

* remove global dependency constraints ([9b13319](https://github.com/mxenabled/path-facilities/commit/9b13319ab5775566c7e61c79229c4d3ba0512237))


### Miscellaneous Chores

* release 1.3.2-SNAPSHOT ([75df3f4](https://github.com/mxenabled/path-facilities/commit/75df3f49f6fb499e3f33f5e977cea66eb6134207))

## [1.3.2](https://github.com/mxenabled/path-facilities/compare/1.3.1...v1.3.2) (2022-12-13)


### Bug Fixes

* pin netty to 4.1.86.Final for CVE-2022-41915 ([d8199bd](https://github.com/mxenabled/path-facilities/commit/d8199bdbcec46eb57a099997075d065577a1cb31))
* syntax ([d8199bd](https://github.com/mxenabled/path-facilities/commit/d8199bdbcec46eb57a099997075d065577a1cb31))

## [1.3.1](https://github.com/mxenabled/path-facilities/compare/1.3.0...1.3.1) (2022-11-03)


### Bug Fixes

* subscribe publish to release publish event ([6d04a62](https://github.com/mxenabled/path-facilities/commit/6d04a62152d58232b8faeb8b374dd50d97bae722))

## [1.3.0](https://github.com/mxenabled/path-facilities/compare/1.2.0...1.3.0) (2022-11-03)


### Features

* bump version to deploy to maven central ([d947495](https://github.com/mxenabled/path-facilities/commit/d94749578705d6299976f09377c898e5b66b670c))
* publish platform ([bbad50e](https://github.com/mxenabled/path-facilities/commit/bbad50e2e293c9771a7c80a1802d26dfccd9f2bc))

## [1.2.0](https://github.com/mxenabled/path-facilities/compare/v1.1.0...1.2.0) (2022-10-27)


### Features

* publish javadoc and sources artifact ([8c2fd7b](https://github.com/mxenabled/path-facilities/commit/8c2fd7bf5f62b1f04f84f0a7696b4e6c46e6b371))

## [1.1.0](https://github.com/mxenabled/path-facilities/compare/v1.0.0...v1.1.0) (2022-10-19)


### Features

* adjust store-vault exceptions ([b7fef60](https://github.com/mxenabled/path-facilities/commit/b7fef60eb354ea08d6e979200e6ae39579e8b23e))
* improve encryption-service-vault exceptions ([5cb53d6](https://github.com/mxenabled/path-facilities/commit/5cb53d6b7f8e6a280130d3447e8da2328433aaff))
* improve jasypt encryption service exceptions ([1b01b9d](https://github.com/mxenabled/path-facilities/commit/1b01b9da6d371ef5a9e0b412e82cc4f575fbc347))
* improve NATS message broker exceptions ([18a7c9d](https://github.com/mxenabled/path-facilities/commit/18a7c9de06470808fc02505d2c1616c4ca6cd7a7))
* improve store-redis exceptions ([829b29c](https://github.com/mxenabled/path-facilities/commit/829b29ca7380abd9680675c76b1418d0e29e3d49))

## [1.0.0](https://github.com/mxenabled/path-facilities/compare/0.0.2...v1.0.0) (2022-10-07)


### ⚠ BREAKING CHANGES

* upgrade SDK to v1.0.0

### Code Refactoring

* upgrade SDK to v1.0.0 ([88a5f3f](https://github.com/mxenabled/path-facilities/commit/88a5f3f93b5f32bc25742a22641d62f0bc90c7a5))

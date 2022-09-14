[![Release](https://jitpack.io/v/mxenabled/path-facilities.svg)](https://jitpack.io/p/mxenabled/path-facilities)

# Path - Facilities

* [Store/Redis](store/redis/README.md)
* [Store/Vault](store/vault/README.md)
* [encryption-service/Vault](encryption-service/vault/README.md)
* [encryption-service/Jasypt](encryption-service/Jasypt/README.md)
* [Message Broker/Nats](message-broker/nats/README.md)
* [Fault Tolerant Executor/Resilience4J](fault-tolerant-executor/resilience4j/README.md)
* [Exception Reporter/Honeybadger](exception-reporter/honeybadger/README.md)

## Contributing
Create a topic branch. Make our changes commit and push to GitLab. Create an MR.

## Building everything

```shell
$ ./gradlew clean build
```

## Assembling everything

```shell
$ ./gradlew clean assemble
```

## Formatting everything

```shell
$ ./gradlew spotlessApply
```

## Testing everything

```shell
$ ./gradlew clean test
```

## Building, assembling, formatting, and testing a subproject

Via helper scripts:

```shell
$ ./bin/build $projectName
```

```shell
$ ./bin/assemble $projectName
```

```shell
$ ./bin/format $projectName
```

```shell
$ ./bin/test $projectName
```

Via gradlew:

```shell
$ ./gradlew :$projectName:clean :$projectName:build
```

```shell
$ ./gradlew :$projectName:clean :$projectName:assemble
```

```shell
$ ./gradlew :$projectName:spotlessApply
```

```shell
$ ./gradlew :$projectName:clean :$projectName:test
```

## Managing Dependencies

This project uses gradle to manage dependencies with dependency locking. The Vogue plugin is used to help keep the dependencies up-to-date. [Vogue](https://gitlab.mx.com/path/vogue)

**Generating lockfiles for all projects**

```shell
$ ./gradlew assemble --write-locks
```

**Generating lockfiles for a single project**

```shell
$ ./gradlew :$projectName:dependencies --write-locks
```

**View out-of-date dependencies**

```shell
$ ./gradlew vogueReport
```

**Determine source of dependency**

```shell
$ ./gradlew dependencies
```

**Scan dependencies for vulnerabilities**

```shell
$ ./gradlew dependencyCheckAnalyze
```
To view the generated report of found vulnerabilities open `build/reports/dependency-check-report.html` in a browser.

## Deploying Locally

To create a local build of the accessor to use in connector services use

```shell
$ ./gradlew publishToMavenLocal
```
This will create a local build in your local maven repository that you can
then reference in other services.

On OXS using gradle the default location for the local maven repository is
```shell
~/.m2/repository/
```

## Deploying

* Merge Pull Request to Master
* Switch to `master` branch
* Update version in `build.gradle` (the version must be unique)
* Commit the updated `build.gradle`
    * `git add build.gradle&&git commit -m "Bump version to ?.?.?"`
* Push build.gradle update
    * `git push origin master`
* Release it `./gradlew release`

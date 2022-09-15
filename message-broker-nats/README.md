# NATS Messaging

## Usage

### Include

```
implementation "com.mx.messaging:mdx-dependency-messaging-nats:1.+"
```

### Configure

Example Development/Sand

```yaml
# Nats configuration
nats:
  enabled: true
  servers: ${NATS_HOSTS}
  dispatcher_count: 1
  timeout_in_milliseconds: 10000
```

Example QA/Int/Prod

```yaml
# Nats configuration
nats:
  enabled: true
  servers: ${NATS_HOSTS}
  dispatcher_count: 2
  timeout_in_milliseconds: 10000
  tls_ca_cert_path: ${NATS_TLS_CA_CERT}
  tls_client_cert_path: ${NATS_TLS_CLIENT_CERT}
  tls_client_key_path: ${NATS_TLS_CLIENT_KEY}
```

**Scan dependencies for vulnerabilities**

```shell
$ ./gradlew dependencyCheckAnalyze
```
To view the generated report of found vulnerabilities open `build/reports/dependency-check-report.html` in a browser.

## Deploying Locally

To create a local build of the accessor to use in connector services use

```shell
$ ./gradlew install
```
This will create a local build in your local maven repository that you can
then reference in other services.

On OXS using gradle the default location for the local maven repository is
```shell
~/.m2/repository/com/mx/path/service/accesssor/qolo/
```

## Deploying

* Merge Pull Request to Master
* Switch to `master` branch
* Update version in `build.gradle` (the version must be unique)
* Commit the updated `build.gradle`
    * `git add build.gradle&&git commit -m "Bump version to ?.?.?"`
* Push build.gradle update
    * `git push origin master`
* Release it `rake release`

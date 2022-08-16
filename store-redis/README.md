[![pipeline status](https://gitlab.mx.com/mx/java-mdx-dependency-session-store-redis/badges/master/pipeline.svg)](https://gitlab.mx.com/mx/java-mdx-dependency-session-store-redis/commits/master)
[![coverage report](https://gitlab.mx.com/mx/java-mdx-dependency-session-store-redis/badges/master/coverage.svg)](https://gitlab.mx.com/mx/java-mdx-dependency-session-store-redis/commits/master)

[Path SDK Issues](https://gitlab.mx.com/groups/mx/money-experiences/path/-/issues?scope=all&utf8=%E2%9C%93&state=opened&label_name[]=Path%20SDK)

# Lettuce Session Store

Exposes `SessionStore` implementation as a Bean that uses the Lettuce library (Redis) for session state storage.

## Gateway Configuration

```json
{
  "store": {
    "class": "com.mx.redis.RedisStore",
    "configurations": {
      "host": "localhost",
      "port": 6379,
      "connectionTimeoutSeconds": 10,
      "computationThreadPoolSize": 5,
      "ioThreadPoolSize": 5
    }
  }
}
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

# Facility - Fault Tolerant Executor - Resilience4j

`FaultTolerantExecutor` implementation for [Resilience4j](https://resilience4j.readme.io/docs/getting-started).

## Usage

This is meant to be a low-level facility used to wrap volatile tasks (like network communication) in a layer of fault-tolerant
protection. This means we can do things like: limit max concurrency, kill slow tasks that exceed a configured time limit,
prevent tasks from being submitted if there are too many failures, etc.

First, retrieve the client-configured `FaultTolerantExecutor` from the `Facilities` class.

```java
FaultTolerantExecutor executor = Facilities.getFaultTolerantExecutor(clientId);
```

Next, submit a task to be executed with fault-tolerant protections:

```java
try {
   executor.submit("http.identity", () -> networkRequest.execute());
} catch (FaultTolerantExecutionException e) {
    // Handle exceptions in whatever way makes the most sense.
}
```

## Gateway Configuration

There are four separate utilities that can be configured:

* [Bulkheads - limit max concurrency (counting semaphore)](https://resilience4j.readme.io/docs/bulkhead)
* [Circuit Breakers - detect reoccurring failures and prevent task execution](https://resilience4j.readme.io/docs/circuitbreaker)
* [Time Limiters - limit the amount of time a task can execute for](https://resilience4j.readme.io/docs/timeout)
* [ThreadPoolBulkhead - limit max concurrency (thread pool)](https://resilience4j.readme.io/docs/bulkhead#create-and-configure-a-threadpoolbulkhead)

```yaml
demo: # the Client ID
  facilities:
    faultTolerantExecutor:
      class: package.path.to.Resilience4jFaultTolerantExecutor
      configurations:

        # Default configurations. These will be used if the scope provided doesn't have a match.
        defaults:
          circuitBreaker:
            enabled: true
            failureRateThreshold: 25
            minimumNumberOfCalls: 70
            slidingWindowType: COUNT_BASED
          bulkhead:
            enabled: true
            maxConcurrentCalls: 2
          timeLimiter:
            enabled: true
            timeoutMillis: 500
          # Note: You should generally either configure a 'bulkhead' or a 'threadPoolBulkhead', but not both.
          #       This is simply to provide a configuration sample for both bulkheads.
          threadPoolBulkhead:
            enabled: true
            coreThreadPoolSize: 10
            maxThreadPoolSize: 30
            queueCapacity: 50
            keepAliveMillis: 20

        # Custom configuration scopes. If the provided scope matches (fully or partially) one of these configurations,
        # then the custom configuration profile will be used instead of the defaults.

        # Note: These configurations effectively override the default configurations.
        #       Any configurations not overridden in a custom scope will use the default configurations.
        scopes:

          - scope: http.identity
            configurations:
              circuitBreaker:
                enabled: true
                failureRateThreshold: 90
                minimumNumberOfCalls: 25
                slidingWindowType: TIME_BASED
              timeLimiter:
                enabled: true
                timeoutMillis: 1200
              bulkhead:
                enabled: false

          - scope: nats
            configurations:
              timeLimiter:
                enabled: false
              bulkhead:
                enabled: true
                maxConcurrentCalls: 100
```

You can view all the possible configuration fields (as well as their defaults) for each utility in the following files:
* [BulkheadConfigurations.java](src/main/java/com/mx/path/service/facility/fault_tolerant_executor/configuration/BulkheadConfigurations.java)
* [CircuitBreakerConfigurations.java](src/main/java/com/mx/path/service/facility/fault_tolerant_executor/configuration/CircuitBreakerConfigurations.java)
* [TimeLimiterConfigurations.java](src/main/java/com/mx/path/service/facility/fault_tolerant_executor/configuration/TimeLimiterConfigurations.java)
* [ThreadPoolBulkheadConfigurations.java](src/main/java/com/mx/path/service/facility/fault_tolerant_executor/configuration/ThreadPoolBulkheadConfigurations.java)

## Managing Dependencies

This project uses gradle to manage dependencies. The Vogue plugin is used to help keep the dependencies up-to-date. [Vogue](https://github.com/mxenabled/vogue)

**View out-of-date dependencies**

```shell
$ ./gradlew vogueReport
```

**Scan dependencies for vulnerabilities**

```shell
$ ./gradlew dependencyCheckAnalyze
```
To view the generated report of found vulnerabilities open `build/reports/dependency-check-report.html` in a browser.

## Deploying Locally

To create a local build of the accessor to use in connector services use

```shell
$ ./gradlew clean publishToMavenLocal
```
This will create a local build in your local maven repository that you can
then reference in other services.

On OXS using gradle the default location for the local maven repository is
```shell
~/.m2/repository/com/mx/**
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

package com.mx.path.service.facility.fault_tolerant_executor

import java.time.Duration
import java.util.stream.Collectors

import com.mx.path.service.facility.fault_tolerant_executor.configuration.BulkheadConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.CircuitBreakerConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.Configurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.Resilience4jConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.ScopedResilience4jConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.ThreadPoolBulkheadConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.TimeLimiterConfigurations

import spock.lang.Specification
import spock.lang.Unroll

class ScopedConfigurationRegistryTest extends Specification {
  ScopedConfigurationRegistry subject

  def setup() {
    subject = new ScopedConfigurationRegistry()
  }

  def "initializeFromConfigurations"() {
    given:
    def configurations = buildConfigurations()

    when:
    subject.initializeFromConfigurations(configurations)

    then:
    subject.defaultConfigurations.scope == "DEFAULT"
    subject.defaultConfigurations.configurations.bulkheadConfigurations.maxConcurrentCalls == 1
    subject.defaultConfigurations.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(2)
    subject.defaultConfigurations.configurations.circuitBreakerConfigurations.failureRateThreshold == 3
    subject.defaultConfigurations.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(4)

    subject.configurationRegistry
        .stream()
        .map({ config -> config.getScope() })
        .collect(Collectors.toList()) == [
          "http.identity.create",
          "http.remote_deposit",
          "http.profile.get",
          "http.identity",
          "DEFAULT",
          "http",
          "nats"
        ]

    def nats = findConfigForScope(subject, "nats")
    nats.scope == "nats"
    nats.configurations.bulkheadConfigurations.maxConcurrentCalls == 21
    nats.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(22)
    nats.configurations.circuitBreakerConfigurations.failureRateThreshold == 23
    nats.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 17
    nats.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(24)
    nats.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(24)

    def http = findConfigForScope(subject, "http")
    http.scope == "http"
    http.configurations.bulkheadConfigurations.maxConcurrentCalls == 17
    http.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(18)
    http.configurations.circuitBreakerConfigurations.failureRateThreshold == 19
    http.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 17
    http.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(20)
    http.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(20)

    def defaultScope = findConfigForScope(subject, "DEFAULT")
    defaultScope.scope == "DEFAULT"
    defaultScope.configurations.bulkheadConfigurations.maxConcurrentCalls == 1
    defaultScope.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(2)
    defaultScope.configurations.circuitBreakerConfigurations.failureRateThreshold == 3
    defaultScope.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 17
    defaultScope.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(4)
    defaultScope.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(4)

    def httpIdentity = findConfigForScope(subject, "http.identity")
    httpIdentity.scope == "http.identity"
    httpIdentity.configurations.bulkheadConfigurations.maxConcurrentCalls == 25
    httpIdentity.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(26)
    httpIdentity.configurations.circuitBreakerConfigurations.failureRateThreshold == 27
    httpIdentity.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 1
    httpIdentity.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(28)
    httpIdentity.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(28)

    def httpProfileGet = findConfigForScope(subject, "http.profile.get")
    httpProfileGet.scope == "http.profile.get"
    httpProfileGet.configurations.bulkheadConfigurations.maxConcurrentCalls == 9
    httpProfileGet.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(10)
    httpProfileGet.configurations.circuitBreakerConfigurations.failureRateThreshold == 11
    httpProfileGet.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 17
    httpProfileGet.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(12)
    httpProfileGet.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(12)

    def httpRemoteDeposit = findConfigForScope(subject, "http.remote_deposit")
    httpRemoteDeposit.scope == "http.remote_deposit"
    httpRemoteDeposit.configurations.bulkheadConfigurations.maxConcurrentCalls == 13
    httpRemoteDeposit.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(14)
    httpRemoteDeposit.configurations.circuitBreakerConfigurations.failureRateThreshold == 15
    httpRemoteDeposit.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 10
    httpRemoteDeposit.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(16)
    httpRemoteDeposit.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(16)

    def httpIdentityCreate = findConfigForScope(subject, "http.identity.create")
    httpIdentityCreate.scope == "http.identity.create"
    httpIdentityCreate.configurations.bulkheadConfigurations.maxConcurrentCalls == 5
    httpIdentityCreate.configurations.bulkheadConfigurations.maxWaitDuration == Duration.ofMillis(6)
    httpIdentityCreate.configurations.circuitBreakerConfigurations.failureRateThreshold == 7
    httpIdentityCreate.configurations.circuitBreakerConfigurations.minimumNumberOfCalls == 17
    httpIdentityCreate.configurations.timeLimiterConfigurations.timeoutDuration == Duration.ofMillis(8)
    httpIdentityCreate.configurations.threadPoolBulkheadConfigurations.keepAlive == Duration.ofMillis(8)
  }

  def "getCircuitBreaker"() {
    given:
    subject.initializeFromConfigurations(buildConfigurations())

    when: "a fully qualified scope is provided and matched"
    def circuitBreaker = subject.getCircuitBreaker("http.identity.create")

    then:
    circuitBreaker != null
    circuitBreaker.name == "http.identity.create"
    circuitBreaker.circuitBreakerConfig.failureRateThreshold == 7
    circuitBreaker.circuitBreakerConfig.minimumNumberOfCalls == 17
    circuitBreaker.circuitBreakerConfig.slidingWindowSize == 100

    when: "a fully qualified scope is provided and partially matched"
    circuitBreaker = subject.getCircuitBreaker("http.profile.create")

    then:
    circuitBreaker != null
    circuitBreaker.name == "http"
    circuitBreaker.circuitBreakerConfig.failureRateThreshold == 19
    circuitBreaker.circuitBreakerConfig.minimumNumberOfCalls == 17
    circuitBreaker.circuitBreakerConfig.slidingWindowSize == 100

    when: "a fully qualified scope is provided and not matched"
    circuitBreaker = subject.getCircuitBreaker("some.other.scope")

    then:
    circuitBreaker != null
    circuitBreaker.name == "DEFAULT"
    circuitBreaker.circuitBreakerConfig.failureRateThreshold == 3
    circuitBreaker.circuitBreakerConfig.minimumNumberOfCalls == 17
    circuitBreaker.circuitBreakerConfig.slidingWindowSize == 100
  }

  def "getBulkhead"() {
    given:
    subject.initializeFromConfigurations(buildConfigurations())

    when: "a fully qualified scope is provided and matched"
    def bulkhead = subject.getBulkhead("http.profile.get")

    then:
    bulkhead != null
    bulkhead.name == "http.profile.get"
    bulkhead.bulkheadConfig.maxConcurrentCalls == 9
    bulkhead.bulkheadConfig.maxWaitDuration == Duration.ofMillis(10)
    bulkhead.bulkheadConfig.writableStackTraceEnabled

    when: "a fully qualified scope is provided and partially matched"
    bulkhead = subject.getBulkhead("nats.ach_transfers.create")

    then:
    bulkhead != null
    bulkhead.name == "nats"
    bulkhead.bulkheadConfig.maxConcurrentCalls == 21
    bulkhead.bulkheadConfig.maxWaitDuration == Duration.ofMillis(22)
    bulkhead.bulkheadConfig.writableStackTraceEnabled

    when: "a fully qualified scope is provided and not matched"
    bulkhead = subject.getBulkhead("some.other.scope")

    then:
    bulkhead != null
    bulkhead.name == "DEFAULT"
    bulkhead.bulkheadConfig.maxConcurrentCalls == 1
    bulkhead.bulkheadConfig.maxWaitDuration == Duration.ofMillis(2)
    bulkhead.bulkheadConfig.writableStackTraceEnabled
  }

  def "getTimeLimiter"() {
    given:
    subject.initializeFromConfigurations(buildConfigurations())

    when: "a fully qualified scope is provided and matched"
    def timeLimiter = subject.getTimeLimiter("http.profile.get")

    then:
    timeLimiter != null
    timeLimiter.name == "http.profile.get"
    timeLimiter.timeLimiterConfig.timeoutDuration == Duration.ofMillis(12)

    when: "a fully qualified scope is provided and partially matched"
    timeLimiter = subject.getTimeLimiter("nats.ach_transfers.create")

    then:
    timeLimiter != null
    timeLimiter.name == "nats"
    timeLimiter.timeLimiterConfig.timeoutDuration == Duration.ofMillis(24)

    when: "a fully qualified scope is provided and not matched"
    timeLimiter = subject.getTimeLimiter("some.other.scope")

    then:
    timeLimiter != null
    timeLimiter.name == "DEFAULT"
    timeLimiter.timeLimiterConfig.timeoutDuration == Duration.ofMillis(4)
  }

  def "getThreadPoolBulkhead"() {
    given:
    subject.initializeFromConfigurations(buildConfigurations())

    when: "a fully qualified scope is provided and matched"
    def bulkhead = subject.getThreadPoolBulkhead("http.profile.get")

    then:
    bulkhead != null
    bulkhead.name == "http.profile.get"
    bulkhead.bulkheadConfig.coreThreadPoolSize == 12
    bulkhead.bulkheadConfig.maxThreadPoolSize == 12
    bulkhead.bulkheadConfig.queueCapacity == 15
    bulkhead.bulkheadConfig.keepAliveDuration == Duration.ofMillis(12)

    when: "a fully qualified scope is provided and partially matched"
    bulkhead = subject.getThreadPoolBulkhead("nats.ach_transfers.create")

    then:
    bulkhead != null
    bulkhead.name == "nats"
    bulkhead.bulkheadConfig.coreThreadPoolSize == 12
    bulkhead.bulkheadConfig.maxThreadPoolSize == 12
    bulkhead.bulkheadConfig.queueCapacity == 15
    bulkhead.bulkheadConfig.keepAliveDuration == Duration.ofMillis(24)

    when: "a fully qualified scope is provided and not matched"
    bulkhead = subject.getThreadPoolBulkhead("some.other.scope")

    then:
    bulkhead != null
    bulkhead.bulkheadConfig.coreThreadPoolSize == 12
    bulkhead.bulkheadConfig.maxThreadPoolSize == 12
    bulkhead.bulkheadConfig.queueCapacity == 15
    bulkhead.bulkheadConfig.keepAliveDuration == Duration.ofMillis(4)
  }

  @Unroll
  def "isValidScopeSyntax"() {
    expect:
    subject.isValidScopeSyntax(scope) == valid

    where:
    scope                      || valid
    "http"                     || true
    "http."                    || false
    "http.profile"             || true
    "http-identity"            || false
    "http.identity.get"        || true
    ".nats"                    || false
    "nats"                     || true
    "nats.remote_deposits.get" || true
  }

  def "findBestConfigurations"() {
    given:
    def registry = new ArrayList<ScopedResilience4jConfigurations>().tap {
      add(new ScopedResilience4jConfigurations().tap { scope = "one.two.three" })
      add(new ScopedResilience4jConfigurations().tap { scope = "one.two.thre3" })
    }

    subject.setConfigurationRegistry(registry)

    when: "two scopes have the same character length, but slightly differ"
    def config = subject.findBestConfigurations("one.two.thre3")

    then:
    config.scope == "one.two.thre3"
  }

  def "getFaultTolerantScopeConfiguration"() {
    given:
    def registry = new ArrayList<ScopedResilience4jConfigurations>().tap {
      add(new ScopedResilience4jConfigurations().tap {
        scope = "one.two.three"
        configurations = Resilience4jConfigurations.builder()
            .timeLimiterConfigurations(
            TimeLimiterConfigurations.builder().
            timeoutDuration(Duration.ofMillis(10000))
            .build().tap { enabled = true })
            .build()
      })
      add(new ScopedResilience4jConfigurations().tap { scope = "one.two.thre3" })
    }

    subject.setConfigurationRegistry(registry)

    when:
    def configurations1 = subject.getFaultTolerantScopeConfiguration("one.two.three")
    def configurations2 = subject.getFaultTolerantScopeConfiguration("one.two.thre3")

    then:
    configurations1.timeout == Duration.ofMillis(10000)
    configurations2.timeout == null
    configurations1 == subject.faultTolerantScopeConfigurationMap.get("one.two.three")
    configurations2 == subject.faultTolerantScopeConfigurationMap.get("one.two.thre3")
  }

  private ScopedResilience4jConfigurations findConfigForScope(ScopedConfigurationRegistry registry, String scope) {
    return registry
        .configurationRegistry
        .stream()
        .filter({ config -> config.scope == scope })
        .findFirst()
        .orElse(null)
  }

  private Configurations buildConfigurations() {
    return new Configurations().tap {
      scopes = new ArrayList<>().tap {
        defaults = buildResilience4jConfigurations(1,2,3,4).tap { circuitBreakerConfigurations.minimumNumberOfCalls = 17 }
        add(new ScopedResilience4jConfigurations().tap {
          scope = "http.identity.create"
          configurations = buildResilience4jConfigurations(5,6,7,8)
        })
        add(new ScopedResilience4jConfigurations().tap {
          scope = "http.profile.get"
          configurations = buildResilience4jConfigurations(9,10,11,12)
        })
        add(new ScopedResilience4jConfigurations().tap {
          scope = "http.remote_deposit"
          configurations = buildResilience4jConfigurations(13,14,15,16).tap { circuitBreakerConfigurations.minimumNumberOfCalls = 10 }
        })
        add(new ScopedResilience4jConfigurations().tap {
          scope = "http"
          configurations = buildResilience4jConfigurations(17,18,19,20)
        })
        add(new ScopedResilience4jConfigurations().tap {
          scope = "nats"
          configurations = buildResilience4jConfigurations(21,22,23,24)
        })
        add(new ScopedResilience4jConfigurations().tap {
          scope = "http.identity"
          configurations = buildResilience4jConfigurations(25,26,27,28).tap { circuitBreakerConfigurations.minimumNumberOfCalls = 1 }
        })
      }
    }
  }

  private buildResilience4jConfigurations(int maxConcurrentCalls, int maxWaitDurationMillis, int failureRateThreshold, int timeoutDurationMillis) {
    return  Resilience4jConfigurations.builder()
        .bulkheadConfigurations(
        BulkheadConfigurations.builder()
        .maxConcurrentCalls(maxConcurrentCalls)
        .maxWaitDuration(Duration.ofMillis(maxWaitDurationMillis))
        .build().tap { enabled = true })
        .circuitBreakerConfigurations(
        CircuitBreakerConfigurations.builder()
        .failureRateThreshold(failureRateThreshold)
        .build().tap { enabled = true })
        .timeLimiterConfigurations(
        TimeLimiterConfigurations.builder()
        .timeoutDuration(Duration.ofMillis(timeoutDurationMillis))
        .build().tap { enabled = true })
        .threadPoolBulkheadConfigurations(ThreadPoolBulkheadConfigurations.builder()
        .maxThreadPoolSize(12)
        .coreThreadPoolSize(12)
        .queueCapacity(15)
        .keepAlive(Duration.ofMillis(timeoutDurationMillis))
        .build().tap { enabled = true })
        .build()
  }
}

package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration

import java.time.Duration

import spock.lang.Specification

class ConfigurationUtilsTest extends Specification {
  ConfigurationUtils subject

  def setup() {
    subject = new ConfigurationUtils()
  }

  def "merges bulkhead configurations and ignores null values"() {
    given:
    def defaultConfig = BulkheadConfigurations.builder()
        .enabled(true)
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ofMillis(100))
        .build()

    def overrideConfig = BulkheadConfigurations.builder()
        .maxConcurrentCalls(10000)
        .build()

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    defaultConfig.enabled
    defaultConfig.maxConcurrentCalls == 10000
    defaultConfig.maxWaitDuration == Duration.ofMillis(100)
  }

  def "merges circuit breaker configurations and ignores null values"() {
    given:
    def defaultConfig = CircuitBreakerConfigurations.builder().build().tap {
      enabled = false
      slidingWindowTaskCount = 123
      slidingWindowDuration = Duration.ofSeconds(1)
    }

    def overrideConfig = CircuitBreakerConfigurations.builder().build().tap {
      enabled = true
      maxWaitDurationInHalfOpenState = Duration.ofMillis(100)
      automaticTransitionFromOpenToHalfOpenEnabled = true
      slidingWindowType = "TIME_BASED"
      slidingWindowDuration = Duration.ofSeconds(10)
    }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    defaultConfig.enabled
    defaultConfig.slidingWindowTaskCount == 123
    defaultConfig.slidingWindowDuration == Duration.ofSeconds(10)
    defaultConfig.maxWaitDurationInHalfOpenState == Duration.ofMillis(100)
    defaultConfig.automaticTransitionFromOpenToHalfOpenEnabled
    defaultConfig.slidingWindowType == "TIME_BASED"
  }

  def "merges time limiter configurations and ignores null values"() {
    given:
    def defaultConfig = TimeLimiterConfigurations.builder().build()

    def overrideConfig = TimeLimiterConfigurations.builder().build().tap { timeoutDuration = Duration.ofMillis(10101) }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    !defaultConfig.enabled
    defaultConfig.timeoutDuration == Duration.ofMillis(10101)
  }

  def "merges thread-pool bulkhead configurations and ignores null values"() {
    given:
    def defaultConfig = ThreadPoolBulkheadConfigurations.builder().build().tap {
      maxThreadPoolSize = 123
      coreThreadPoolSize = 10
    }

    def overrideConfig = ThreadPoolBulkheadConfigurations.builder().build().tap {
      maxThreadPoolSize = 1
      keepAlive = Duration.ofMillis(1234)
    }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    !defaultConfig.enabled
    defaultConfig.maxThreadPoolSize == 1
    defaultConfig.coreThreadPoolSize == 10
    defaultConfig.keepAlive == Duration.ofMillis(1234)
    defaultConfig.queueCapacity == null
  }
}

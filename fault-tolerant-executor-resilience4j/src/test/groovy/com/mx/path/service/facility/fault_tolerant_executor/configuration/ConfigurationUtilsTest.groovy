package com.mx.path.service.facility.fault_tolerant_executor.configuration


import spock.lang.Specification

class ConfigurationUtilsTest extends Specification {
  ConfigurationUtils subject

  def setup() {
    subject = new ConfigurationUtils();
  }

  def "merges bulkhead configurations and ignores null values"() {
    given:
    def defaultConfig = BulkheadConfigurations.builder().build().tap {
      enabled = true
      maxConcurrentCalls = 1
      maxWaitDurationMillis = 100
    }

    def overrideConfig = BulkheadConfigurations.builder().build().tap { maxConcurrentCalls = 10000 }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    defaultConfig.enabled
    defaultConfig.maxConcurrentCalls == 10000
    defaultConfig.maxWaitDurationMillis == 100
  }

  def "merges circuit breaker configurations and ignores null values"() {
    given:
    def defaultConfig = CircuitBreakerConfigurations.builder().build().tap {
      enabled = false
      slidingWindowTaskCount = 123
      slidingWindowDurationSeconds = 1
    }

    def overrideConfig = CircuitBreakerConfigurations.builder().build().tap {
      enabled = true
      maxWaitDurationInHalfOpenStateMillis = 100
      automaticTransitionFromOpenToHalfOpenEnabled = true
      slidingWindowType = "TIME_BASED"
      slidingWindowDurationSeconds = 10
    }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    defaultConfig.enabled
    defaultConfig.slidingWindowTaskCount == 123
    defaultConfig.slidingWindowDurationSeconds == 10
    defaultConfig.maxWaitDurationInHalfOpenStateMillis == 100
    defaultConfig.automaticTransitionFromOpenToHalfOpenEnabled
    defaultConfig.slidingWindowType == "TIME_BASED"
  }

  def "merges time limiter configurations and ignores null values"() {
    given:
    def defaultConfig = TimeLimiterConfigurations.builder().build()

    def overrideConfig = TimeLimiterConfigurations.builder().build().tap { timeoutDurationMillis = 10101 }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    !defaultConfig.enabled
    defaultConfig.timeoutDurationMillis == 10101
  }

  def "merges thread-pool bulkhead configurations and ignores null values"() {
    given:
    def defaultConfig = ThreadPoolBulkheadConfigurations.builder().build().tap {
      maxThreadPoolSize = 123
      coreThreadPoolSize = 10
    }

    def overrideConfig = ThreadPoolBulkheadConfigurations.builder().build().tap {
      maxThreadPoolSize = 1
      keepAliveMillis = 1234
    }

    when:
    subject.mergeNonNullProperties(defaultConfig, overrideConfig)

    then:
    !defaultConfig.enabled
    defaultConfig.maxThreadPoolSize == 1
    defaultConfig.coreThreadPoolSize == 10
    defaultConfig.keepAliveMillis == 1234
    defaultConfig.queueCapacity == null
  }
}

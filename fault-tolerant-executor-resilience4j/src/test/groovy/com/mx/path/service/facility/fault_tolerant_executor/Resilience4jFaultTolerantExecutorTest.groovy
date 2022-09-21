package com.mx.path.service.facility.fault_tolerant_executor

import static org.mockito.Mockito.*

import java.time.Duration
import java.util.concurrent.TimeoutException

import com.mx.common.accessors.PathResponseStatus
import com.mx.common.connect.ConnectException
import com.mx.common.connect.ServiceUnavailableException
import com.mx.common.connect.TooManyRequestsException
import com.mx.common.process.FaultTolerantTask
import com.mx.path.service.facility.fault_tolerant_executor.configuration.BulkheadConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.CircuitBreakerConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.Configurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.Resilience4jConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.ThreadPoolBulkheadConfigurations
import com.mx.path.service.facility.fault_tolerant_executor.configuration.TimeLimiterConfigurations

import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException

import spock.lang.Specification
import spock.lang.Unroll

class Resilience4jFaultTolerantExecutorTest extends Specification {
  Resilience4jFaultTolerantExecutor subject

  def setup() {
    subject = spy(new Resilience4jFaultTolerantExecutor(new Configurations()))
  }

  @Unroll
  def "translates exceptions correctly"() {
    given:
    doThrow(originalException).when(subject).executeDecoratorStack(any(String), any(FaultTolerantTask))

    when:
    subject.submit("DEFAULT", { config ->  })

    then:
    def exception = thrown(ConnectException)
    exception.status == failureStatus
    exception.cause == originalException

    where:
    originalException               || failureStatus
    mock(BulkheadFullException)     || PathResponseStatus.TOO_MANY_REQUESTS
    mock(CallNotPermittedException) || PathResponseStatus.UNAVAILABLE
    new TimeoutException()          || PathResponseStatus.TIMEOUT
    new RuntimeException()          || PathResponseStatus.INTERNAL_ERROR
  }

  def "runs a submitted task"() {
    when:
    subject.submit("DEFAULT", { config -> })

    then:
    noExceptionThrown()
  }

  def "passes in FaultTolerantScopeConfiguration"() {
    when: "a timeout is configured"
    new Resilience4jFaultTolerantExecutor(new Configurations().tap {
      defaults.timeLimiterConfigurations.enabled = true
      defaults.timeLimiterConfigurations.timeoutDurationMillis = 10000
    }).submit("DEFAULT", { config ->
      assert config.timeout == Duration.ofMillis(10000)
    })

    then:
    noExceptionThrown()

    when: "a timeout is not configured"
    new Resilience4jFaultTolerantExecutor(new Configurations()).submit("DEFAULT", { config ->
      assert config.timeout == null
    })

    then:
    noExceptionThrown()
  }

  def "rejects tasks if bulkhead is full"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .bulkheadConfigurations(BulkheadConfigurations.builder()
          .maxConcurrentCalls(1)
          .build().tap { enabled = true })
          .build()
    }
    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    def thread1 = new Thread({ -> subject.submit("DEFAULT", { config -> Thread.sleep(500) })})

    thread1.start()
    Thread.sleep(150)

    subject.submit("DEFAULT", { -> })
    thread1.join()

    then:
    def exception = thrown(TooManyRequestsException)
    exception.status == PathResponseStatus.TOO_MANY_REQUESTS
  }

  def "times out a task if it takes too long"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .timeLimiterConfigurations(TimeLimiterConfigurations.builder()
          .timeoutDurationMillis(50)
          .build().tap { enabled = true })
          .build()
    }
    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    subject.submit("DEFAULT", { config -> Thread.sleep(1000)})

    then:
    def exception = thrown(com.mx.common.connect.TimeoutException)
    exception.status == PathResponseStatus.TIMEOUT
  }

  def "circuit breaker opens if there are too many failures in sliding window (COUNT_BASED)"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .circuitBreakerConfigurations(CircuitBreakerConfigurations.builder()
          .failureRateThreshold(50)
          .slidingWindowTaskCount(2)
          .slidingWindowType("COUNT_BASED")
          .minimumNumberOfCalls(1)
          .build().tap { enabled = true })
          .build()
    }

    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    def thread1 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})
    def thread2 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})
    def thread3 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})

    thread1.start()
    thread2.start()
    thread3.start()
    Thread.sleep(150)

    subject.submit("DEFAULT", { -> })

    thread1.join()
    thread2.join()
    thread3.join()

    then:
    def e = thrown(ServiceUnavailableException)
    e.status == PathResponseStatus.UNAVAILABLE
  }

  def "circuit breaker opens if there are too many failures in sliding window (TIME_BASED)"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .circuitBreakerConfigurations(CircuitBreakerConfigurations.builder()
          .failureRateThreshold(100)
          .slidingWindowDurationSeconds(1)
          .slidingWindowType("TIME_BASED")
          .minimumNumberOfCalls(1)
          .build().tap { enabled = true })
          .build()
    }

    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    def thread1 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})
    def thread2 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})
    def thread3 = new Thread({ config -> subject.submit("DEFAULT", { -> throw new RuntimeException() })})

    thread1.start()
    thread2.start()
    thread3.start()
    Thread.sleep(150)

    subject.submit("DEFAULT", { -> })

    thread1.join()
    thread2.join()
    thread3.join()

    then:
    def e = thrown(ServiceUnavailableException)
    e.status == PathResponseStatus.UNAVAILABLE
  }

  def "rejects tasks if thread-pool bulkhead is full"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .threadPoolBulkheadConfigurations(ThreadPoolBulkheadConfigurations.builder()
          .maxThreadPoolSize(1)
          .coreThreadPoolSize(1)
          .queueCapacity(1)
          .build().tap { enabled = true })
          .build()
    }
    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    def thread1 = new Thread({ -> subject.submit("DEFAULT", { config -> Thread.sleep(500) })})
    def thread2 = new Thread({ -> subject.submit("DEFAULT", { config -> Thread.sleep(500) })})

    thread1.start()
    thread2.start()
    Thread.sleep(150)

    subject.submit("DEFAULT", { config -> })
    thread1.join()
    thread2.join()

    then:
    def exception = thrown(TooManyRequestsException)
    exception.status == PathResponseStatus.TOO_MANY_REQUESTS
  }

  def "thread-pool bulkhead queues tasks"() {
    given:
    def configurations = new Configurations().tap {
      defaults = Resilience4jConfigurations.builder()
          .threadPoolBulkheadConfigurations(ThreadPoolBulkheadConfigurations.builder()
          .maxThreadPoolSize(1)
          .coreThreadPoolSize(1)
          .queueCapacity(2)
          .build().tap { enabled = true })
          .build()
    }
    subject = new Resilience4jFaultTolerantExecutor(configurations)

    when:
    def thread1 = new Thread({ -> subject.submit("DEFAULT", { config -> Thread.sleep(500) })})
    def thread2 = new Thread({ -> subject.submit("DEFAULT", { config -> Thread.sleep(500) })})

    thread1.start()
    thread2.start()
    Thread.sleep(150)

    subject.submit("DEFAULT", { config -> })
    thread1.join()
    thread2.join()

    then:
    noExceptionThrown()
  }
}

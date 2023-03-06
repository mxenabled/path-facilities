package com.mx.path.service.facility.fault_tolerant_executor.configuration;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.mx.common.configuration.ConfigurationField;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public final class CircuitBreakerConfigurations implements PopulatesDefaults<CircuitBreakerConfigurations> {
  private static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;
  private static final int DEFAULT_SLOW_CALL_RATE_THRESHOLD = 100;
  private static final int DEFAULT_SLOW_CALL_DURATION_THRESHOLD_MILLIS = 30000;
  private static final int DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE = 10;
  private static final int DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE_MILLIS = 0;
  private static final String DEFAULT_SLIDING_WINDOW_TYPE = "COUNT_BASED";
  private static final int DEFAULT_SLIDING_WINDOW_TASK_COUNT = 100;
  private static final int DEFAULT_SLIDING_WINDOW_DURATION_SECONDS = 10;
  private static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100;
  private static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE_MILLIS = 60000;
  private static final boolean DEFAULT_AUTOMATIC_TRANSITION_FROM_OPEN_TO_HALF_OPEN_ENABLED = false;

  /**
   * Determines whether a circuit breaker is enabled or not.
   */
  @ConfigurationField
  private Boolean enabled;

  /**
   * Configures the failure rate threshold in percentage.
   *
   * When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.
   */
  @ConfigurationField
  private Integer failureRateThreshold;

  /**
   * Configures a threshold in percentage. The CircuitBreaker considers a call as slow when the call duration is greater
   * than slowCallDurationThreshold
   *
   * When the percentage of slow calls is equal or greater the threshold, the CircuitBreaker transitions to open and
   * starts short-circuiting calls.
   */
  @ConfigurationField
  private Integer slowCallRateThreshold;

  /**
   * Configures the duration threshold above which calls are considered as slow and increase the rate of slow calls.
   */
  @ConfigurationField
  private Duration slowCallDurationThreshold;

  /**
   * Configures the number of permitted calls when the CircuitBreaker is half open.
   *
   * See: https://resilience4j.readme.io/docs/circuitbreaker
   */
  @ConfigurationField
  private Integer permittedNumberOfCallsInHalfOpenState;

  /**
   * Configures a maximum wait duration which controls the longest amount of time a CircuitBreaker could stay in Half Open state,
   * before it switches to open.
   *
   * Value 0 means the CircuitBreaker would wait infinitely in HalfOpen State until all permitted calls have been completed.
   */
  @ConfigurationField
  private Duration maxWaitDurationInHalfOpenState;

  /**
   * Configures the type of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.
   * Sliding window can either be count-based or time-based.
   *
   * If the sliding window is COUNT_BASED, the last slidingWindowSize calls are recorded and aggregated.
   * If the sliding window is TIME_BASED, the calls of the last slidingWindowSize seconds recorded and aggregated.
   */
  @ConfigurationField
  private String slidingWindowType;

  /**
   * Configures the size of the sliding window (task count) which is used to record the outcome of calls when the CircuitBreaker is closed.
   *
   * Must be provided when slidingWindowType == COUNT_BASED
   */
  @ConfigurationField
  private Integer slidingWindowTaskCount;

  /**
   * Configures the size of the sliding window (seconds) which is used to record the outcome of calls when the CircuitBreaker is closed.
   *
   * Must be provided when slidingWindowType == TIME_BASED
   */
  @ConfigurationField
  private Duration slidingWindowDuration;

  /**
   * Configures the minimum number of calls which are required (per sliding window period) before the CircuitBreaker can
   * calculate the error rate or slow call rate.
   *
   * For example, if minimumNumberOfCalls is 10, then at least 10 calls must be recorded, before the failure rate can be calculated.
   * If only 9 calls have been recorded the CircuitBreaker will not transition to open even if all 9 calls have failed.
   */
  @ConfigurationField
  private Integer minimumNumberOfCalls;

  /**
   * The time that the CircuitBreaker should wait before transitioning from open to half-open.
   */
  @ConfigurationField
  private Duration waitDurationInOpenState;

  /**
   * If set to true it means that the CircuitBreaker will automatically transition from open to half-open state and no call
   * is needed to trigger the transition. A thread is created to monitor all the instances of CircuitBreakers to transition
   * them to HALF_OPEN once waitDurationInOpenState passes.
   *
   * Whereas, if set to false the transition to HALF_OPEN only happens
   * if a call is made, even after waitDurationInOpenState is passed. The advantage here is no thread monitors the state
   * of all CircuitBreakers.
   */
  @ConfigurationField
  private Boolean automaticTransitionFromOpenToHalfOpenEnabled;

  @Override
  public CircuitBreakerConfigurations withDefaults() {
    failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
    slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;
    slowCallDurationThreshold = Duration.ofMillis(DEFAULT_SLOW_CALL_DURATION_THRESHOLD_MILLIS);
    permittedNumberOfCallsInHalfOpenState = DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE;
    maxWaitDurationInHalfOpenState = Duration.ofMillis(DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE_MILLIS);
    slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
    slidingWindowTaskCount = DEFAULT_SLIDING_WINDOW_TASK_COUNT;
    slidingWindowDuration = Duration.ofSeconds(DEFAULT_SLIDING_WINDOW_DURATION_SECONDS);
    minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
    waitDurationInOpenState = Duration.ofMillis(DEFAULT_WAIT_DURATION_IN_OPEN_STATE_MILLIS);
    automaticTransitionFromOpenToHalfOpenEnabled = DEFAULT_AUTOMATIC_TRANSITION_FROM_OPEN_TO_HALF_OPEN_ENABLED;

    return this;
  }
}

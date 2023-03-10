package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration;

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
public final class BulkheadConfigurations implements PopulatesDefaults<BulkheadConfigurations> {
  private static final int DEFAULT_MAX_CONCURRENT_CALLS = 25;
  private static final int DEFAULT_MAX_WAIT_DURATION_MILLIS = 0;

  /**
   * Determines whether a bulkhead is enabled or not.
   */
  @ConfigurationField("enabled")
  private Boolean enabled;

  /**
   * Max amount of parallel executions allowed by the bulkhead.
   */
  @ConfigurationField
  private Integer maxConcurrentCalls;

  /**
   * Max amount of time a thread should be blocked for when attempting to enter a saturated bulkhead.
   */
  @ConfigurationField
  private Duration maxWaitDuration;

  @Override
  public BulkheadConfigurations withDefaults() {
    maxConcurrentCalls = DEFAULT_MAX_CONCURRENT_CALLS;
    maxWaitDuration = Duration.ofMillis(DEFAULT_MAX_WAIT_DURATION_MILLIS);

    return this;
  }
}

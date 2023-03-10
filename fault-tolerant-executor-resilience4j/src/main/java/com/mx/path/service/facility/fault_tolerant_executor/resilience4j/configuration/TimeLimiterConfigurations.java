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
public final class TimeLimiterConfigurations implements PopulatesDefaults<TimeLimiterConfigurations> {
  private static final int DEFAULT_TIMEOUT_DURATION_MILLIS = 30000;

  /**
   * Determines whether a time limiter is enabled or not.
   */
  @ConfigurationField
  private Boolean enabled;

  /**
   * The amount of time that must elapse before a task is timed out and cancelled.
   */
  @ConfigurationField
  private Duration timeoutDuration;

  @Override
  public TimeLimiterConfigurations withDefaults() {
    timeoutDuration = Duration.ofMillis(DEFAULT_TIMEOUT_DURATION_MILLIS);

    return this;
  }
}

package com.mx.path.service.facility.fault_tolerant_executor.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.mx.common.configuration.ConfigurationField;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public final class Resilience4jConfigurations implements PopulatesDefaults<Resilience4jConfigurations> {
  @ConfigurationField("bulkhead")
  @Builder.Default
  private BulkheadConfigurations bulkheadConfigurations = BulkheadConfigurations.builder().build();

  @ConfigurationField("circuitBreaker")
  @Builder.Default
  private CircuitBreakerConfigurations circuitBreakerConfigurations = CircuitBreakerConfigurations.builder().build();

  @ConfigurationField("timeLimiter")
  @Builder.Default
  private TimeLimiterConfigurations timeLimiterConfigurations = TimeLimiterConfigurations.builder().build();

  @ConfigurationField("threadPoolBulkhead")
  @Builder.Default
  private ThreadPoolBulkheadConfigurations threadPoolBulkheadConfigurations = ThreadPoolBulkheadConfigurations.builder().build();

  public Resilience4jConfigurations deepCopy() {
    return toBuilder()
        .bulkheadConfigurations(bulkheadConfigurations.toBuilder().build())
        .circuitBreakerConfigurations(circuitBreakerConfigurations.toBuilder().build())
        .timeLimiterConfigurations(timeLimiterConfigurations.toBuilder().build())
        .threadPoolBulkheadConfigurations(threadPoolBulkheadConfigurations.toBuilder().build())
        .build();
  }

  @Override
  public Resilience4jConfigurations withDefaults() {
    bulkheadConfigurations.withDefaults();
    circuitBreakerConfigurations.withDefaults();
    timeLimiterConfigurations.withDefaults();
    threadPoolBulkheadConfigurations.withDefaults();

    return this;
  }
}

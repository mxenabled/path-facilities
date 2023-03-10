package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration;

import java.util.Objects;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;
import com.mx.common.process.FaultTolerantScopeConfiguration;

@Data
public class ScopedResilience4jConfigurations {
  @ConfigurationField
  private String scope;

  @ConfigurationField
  private Resilience4jConfigurations configurations;

  /**
   * Produces a {@link FaultTolerantScopeConfiguration} that can be consumed by a {@link com.mx.common.process.FaultTolerantTask}
   *
   * @return FaultTolerantScopeConfiguration
   */
  public FaultTolerantScopeConfiguration toFaultTolerantScopeConfiguration() {
    FaultTolerantScopeConfiguration faultTolerantScopeConfiguration = new FaultTolerantScopeConfiguration();

    if (configurations != null
        && Objects.equals(configurations.getTimeLimiterConfigurations().getEnabled(), Boolean.TRUE)
        && configurations.getTimeLimiterConfigurations().getTimeoutDuration() != null) {
      faultTolerantScopeConfiguration.setTimeout(configurations.getTimeLimiterConfigurations().getTimeoutDuration());
    }

    return faultTolerantScopeConfiguration;
  }
}

package com.mx.path.service.facility.fault_tolerant_executor.configuration;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class Configurations {
  @ConfigurationField
  private Resilience4jConfigurations defaults = Resilience4jConfigurations.builder().build();

  @ConfigurationField(elementType = ScopedResilience4jConfigurations.class)
  private List<ScopedResilience4jConfigurations> scopes = new ArrayList<>();
}

package com.mx.path.facility.exception_reporter.honeybadger;

import java.util.Arrays;
import java.util.List;

import com.mx.common.configuration.ConfigurationField;

import lombok.Data;

@Data
public class HoneyBadgerConfiguration {
  @ConfigurationField(required = true)
  private String apiKey;

  @ConfigurationField
  private String packagingPath = "com.mx";

  @ConfigurationField
  private List<String> allowedEnvironments = Arrays.asList("qa", "integration", "production");
}

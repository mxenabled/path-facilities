package com.mx.path.facility.exception_reporter.honeybadger.spring;

import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@SuppressFBWarnings
@Component
@Deprecated
public class HoneyBadgerConfiguration {
  private static Environment environment;
  private static String apiKey;
  private static String packagingPath;
  private static List<String> allowedEnvironmentList = Arrays.asList("qa", "integrations", "production");

  static Environment getEnvironment() {
    return environment;
  }

  @Autowired
  public final void setEnvironment(Environment environment) {
    HoneyBadgerConfiguration.environment = environment;
  }

  static String getApiKey() {
    return apiKey;
  }

  @Value("${honey_badger.api_key:invalid}")
  public final void setApiKey(String apiKey) {
    HoneyBadgerConfiguration.apiKey = apiKey;
  }

  static String getPackagingPath() {
    return packagingPath;
  }

  @Value("${honey_badger.package_path:com.mx}")
  public final void setPackagingPath(String packagingPath) {
    HoneyBadgerConfiguration.packagingPath = packagingPath;
  }

  static List<String> getAllowedEnvironmentList() {
    return allowedEnvironmentList;
  }

  @Value("${honey_badger.allowed_environment_list:qa,sandbox,integration,production}")
  public final void setAllowedEnvironmentList(List<String> allowedEnvironmentList) {
    HoneyBadgerConfiguration.allowedEnvironmentList = allowedEnvironmentList;
  }
}

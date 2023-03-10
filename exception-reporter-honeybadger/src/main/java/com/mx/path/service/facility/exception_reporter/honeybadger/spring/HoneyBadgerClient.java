package com.mx.path.service.facility.exception_reporter.honeybadger.spring;

import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.config.StandardConfigContext;

/**
 * Singleton manager for HoneybadgerReporter instance
 */
@Deprecated
final class HoneyBadgerClient {
  /**
   * Singleton
   */
  private static HoneybadgerReporter honeybadgerReporter;

  static void setHoneybadgerReporter(HoneybadgerReporter honeybadgerReporter) {
    HoneyBadgerClient.honeybadgerReporter = honeybadgerReporter;
  }

  private HoneyBadgerClient() {
  }

  static synchronized HoneybadgerReporter getInstance() {
    if (honeybadgerReporter == null) {
      StandardConfigContext config = new StandardConfigContext();
      config.setApiKey(HoneyBadgerConfiguration.getApiKey()).setEnvironment(HoneyBadgerConfiguration.getEnvironment().getActiveProfiles()[0]).setApplicationPackage(HoneyBadgerConfiguration.getPackagingPath());
      honeybadgerReporter = new HoneybadgerReporter(config);
    }
    return honeybadgerReporter;
  }
}

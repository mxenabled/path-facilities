package com.mx.path.facility.exception_reporter.honeybadger;

import java.util.Locale;

public enum HoneybadgerEnvironment {
  DEVELOPMENT("development"),
  SANDBOX("sandbox"),
  QA("qa"),
  INTEGRATION("integration"),
  PRODUCTION("production");

  private final String value;

  HoneybadgerEnvironment(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  public static HoneybadgerEnvironment resolve(String value) {
    return HoneybadgerEnvironment.valueOf(value.toUpperCase(Locale.ROOT));
  }
}

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

  public HoneybadgerEnvironment ofValue(String value) {
    return Enum.valueOf(HoneybadgerEnvironment.class, value.toUpperCase(Locale.ROOT));
  }
}

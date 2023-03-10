package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration;

public class ConfigurationException extends RuntimeException {
  public ConfigurationException() {
  }

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(Throwable cause) {
    super(cause);
  }

  public ConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

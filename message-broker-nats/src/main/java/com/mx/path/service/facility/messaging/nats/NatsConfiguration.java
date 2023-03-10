package com.mx.path.service.facility.messaging.nats;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import com.mx.common.configuration.ConfigurationField;

@Data
public class NatsConfiguration {
  private static final int DEFAULT_DISPATCHER_COUNT = 1;
  private static final boolean DEFAULT_ENABLED = true;
  private static final String DEFAULT_SERVERS = "nats://127.0.0.1:4222";
  private static final int DEFAULT_TIMEOUT_IN_MILLISECONDS = 10000;
  private static final String DEFAULT_TLS_CA_CERT_PATH = null;
  private static final String DEFAULT_TLS_CLIENT_CERT_PATH = null;
  private static final String DEFAULT_TLS_CLIENT_KEY_PATH = null;
  private static final boolean DEFAULT_TLS_DISABLED = false;

  @ConfigurationField
  private int dispatcherCount = DEFAULT_DISPATCHER_COUNT;

  @ConfigurationField
  private boolean enabled = DEFAULT_ENABLED;

  @ConfigurationField(required = true, placeholder = "nats://127.0.0.1:4222")
  private String servers = DEFAULT_SERVERS;

  /**
   * @deprecated use timeout
   */
  @Deprecated
  @ConfigurationField
  @Getter(AccessLevel.PRIVATE)
  private Integer timeoutInMilliseconds = null;

  @ConfigurationField
  private Duration timeout = null;

  @ConfigurationField
  private String tlsCaCertPath = DEFAULT_TLS_CA_CERT_PATH;

  @ConfigurationField
  private String tlsClientCertPath = DEFAULT_TLS_CLIENT_CERT_PATH;

  @ConfigurationField
  private String tlsClientKeyPath = DEFAULT_TLS_CLIENT_KEY_PATH;

  @ConfigurationField
  private boolean tlsDisabled = DEFAULT_TLS_DISABLED;

  /**
   * Maintain backward compatibility. Remove with timeoutInMilliseconds
   * @return timeout as a Duration
   */
  public final Duration getTimeout() {
    if (timeout == null) {
      if (timeoutInMilliseconds != null) {
        timeout = Duration.ofMillis(timeoutInMilliseconds);
      } else {
        timeout = Duration.ofMillis(DEFAULT_TIMEOUT_IN_MILLISECONDS);
      }
    }

    return timeout;
  }
}

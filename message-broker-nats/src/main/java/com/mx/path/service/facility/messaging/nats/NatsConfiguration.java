package com.mx.path.service.facility.messaging.nats;

import java.time.Duration;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class NatsConfiguration {
  private static final int DEFAULT_DISPATCHER_COUNT = 1;
  private static final boolean DEFAULT_ENABLED = true;
  private static final String DEFAULT_SERVERS = "nats://127.0.0.1:4222";
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(10000);
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

  @ConfigurationField
  private Duration timeout = DEFAULT_TIMEOUT;

  @ConfigurationField
  private String tlsCaCertPath = DEFAULT_TLS_CA_CERT_PATH;

  @ConfigurationField
  private String tlsClientCertPath = DEFAULT_TLS_CLIENT_CERT_PATH;

  @ConfigurationField
  private String tlsClientKeyPath = DEFAULT_TLS_CLIENT_KEY_PATH;

  @ConfigurationField
  private boolean tlsDisabled = DEFAULT_TLS_DISABLED;
}

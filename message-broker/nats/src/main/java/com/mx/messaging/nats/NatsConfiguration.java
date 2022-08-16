package com.mx.messaging.nats;

import lombok.Getter;

import com.mx.common.collections.ObjectMap;

public class NatsConfiguration {
  private static final int DEFAULT_DISPATCHER_COUNT = 1;
  private static final boolean DEFAULT_ENABLED = false;
  private static final String DEFAULT_SERVERS = "nats://127.0.0.1:4222";
  private static final int DEFAULT_TIMEOUT_IN_MILLISECONDS = 10000;
  private static final String DEFAULT_TLS_CA_CERT_PATH = null;
  private static final String DEFAULT_TLS_CLIENT_CERT_PATH = null;
  private static final String DEFAULT_TLS_CLIENT_KEY_PATH = null;
  private static final boolean DEFAULT_TLS_DISABLED = false;

  @Getter
  private final ObjectMap configurations;

  public NatsConfiguration(ObjectMap configurations) {
    if (configurations == null) {
      configurations = new ObjectMap();
    }
    this.configurations = configurations;
  }

  public final int getDispatcherCount() {
    return configurations.getAsInteger("dispatcherCount", DEFAULT_DISPATCHER_COUNT);
  }

  public final boolean isEnabled() {
    return configurations.getAsBoolean("enabled", DEFAULT_ENABLED);
  }

  public final String getServers() {
    return configurations.getAsString("servers", DEFAULT_SERVERS);
  }

  public final int getTimeoutInMilliseconds() {
    return configurations.getAsInteger("timeoutInMilliseconds", DEFAULT_TIMEOUT_IN_MILLISECONDS);
  }

  public final String getTlsCaCertPath() {
    return configurations.getAsString("tlsCaCertPath", DEFAULT_TLS_CA_CERT_PATH);
  }

  public final String getTlsClientCertPath() {
    return configurations.getAsString("tlsClientCertPath", DEFAULT_TLS_CLIENT_CERT_PATH);
  }

  public final String getTlsClientKeyPath() {
    return configurations.getAsString("tlsClientKeyPath", DEFAULT_TLS_CLIENT_KEY_PATH);
  }

  public final boolean getTlsDisabled() {
    return configurations.getAsBoolean("tlsDisabled", DEFAULT_TLS_DISABLED);
  }
}

package com.mx.redis;

import java.time.Duration;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class RedisStoreConfiguration {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_COMPUTATION_THREAD_POOL_SIZE = 5;
  private static final int DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS = 10;
  private static final int DEFAULT_PORT = 6379;
  private static final int DEFAULT_THREAD_POOL_SIZE = 5;

  @ConfigurationField
  private String host = DEFAULT_HOST;

  @ConfigurationField
  private int computationThreadPoolSize = DEFAULT_COMPUTATION_THREAD_POOL_SIZE;

  /**
   * @deprecated Use connectionTimeout
   */
  @Deprecated
  @ConfigurationField
  private Integer connectionTimeoutSeconds = null;

  @ConfigurationField
  private Duration connectionTimeout = null;

  @ConfigurationField
  private int ioThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;

  @ConfigurationField
  private int port = DEFAULT_PORT;

  /**
   * Maintaining backward compatibility. Remove after connectionTimeoutSeconds is removed.
   * @return connectionTimeout
   */
  public Duration getConnectionTimeout() {
    if (connectionTimeout == null) {
      if (connectionTimeoutSeconds != null) {
        this.connectionTimeout = Duration.ofSeconds(connectionTimeoutSeconds);
      } else {
        this.connectionTimeout = Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS);
      }
    }

    return connectionTimeout;
  }
}

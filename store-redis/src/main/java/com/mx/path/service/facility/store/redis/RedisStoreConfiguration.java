package com.mx.path.service.facility.store.redis;

import java.time.Duration;

import lombok.Data;

import com.mx.path.core.common.configuration.ConfigurationField;

@Data
public class RedisStoreConfiguration {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_COMPUTATION_THREAD_POOL_SIZE = 5;
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  private static final int DEFAULT_PORT = 6379;
  private static final int DEFAULT_THREAD_POOL_SIZE = 5;
  private static final boolean DEFAULT_SSL = false;
  private static final boolean DEFAULT_CLUSTER = false;

  @ConfigurationField
  private String host = DEFAULT_HOST;

  @ConfigurationField
  private int computationThreadPoolSize = DEFAULT_COMPUTATION_THREAD_POOL_SIZE;

  @ConfigurationField
  private Duration timeout = DEFAULT_TIMEOUT;

  @ConfigurationField
  private int ioThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;

  @ConfigurationField
  private int port = DEFAULT_PORT;

  @ConfigurationField
  private boolean ssl = DEFAULT_SSL;

  @ConfigurationField
  private boolean cluster = DEFAULT_CLUSTER;

  @ConfigurationField
  private String certFile;

  @ConfigurationField
  private String keyFile;

  @ConfigurationField
  private String passwordFile;
}

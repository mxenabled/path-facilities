package com.mx.path.facility.store.vault;

import java.time.Duration;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class VaultStoreConfiguration {

  private static final String DEFAULT_AUTHENTICATION = "APPROLE";
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final String PUT_IF_NOT_EXIST_UNSUPPORTED = "Put if not exist operations are not supported with Vault";
  private static final String SET_UNSUPPORTED = "Sets are not supported with Vault";
  private static final String TTL_UNSUPPORTED = "TTL is not supported with Vault";
  private static final int DEFAULT_ENGINE_VERSION = 2;
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final int DEFAULT_RETRY_INTERVAL_MILLISECONDS = 200;
  private static final int KEY_NOT_FOUND = 404;
  private static final int MAXIMUM_REAUTHENTICATION_RETRIES = 3;

  @ConfigurationField(value = "app-id")
  private String appId;

  @ConfigurationField(value = "app-role")
  private String appRole;

  @ConfigurationField
  private String authentication = DEFAULT_AUTHENTICATION;

  @ConfigurationField
  private int engineVersion = DEFAULT_ENGINE_VERSION;

  @ConfigurationField
  private int maxRetries = DEFAULT_MAX_RETRIES;

  /**
   * @deprecated use retryInterval
   */
  @Deprecated
  @ConfigurationField
  private Integer retryIntervalMilliseconds = null;

  @ConfigurationField
  private Duration retryInterval = null;

  @ConfigurationField
  private String secretId;

  @ConfigurationField
  private String token;

  @ConfigurationField
  private String uri = DEFAULT_URI;

  @ConfigurationField(value = "user-id")
  private String userId;

  /**
   * For backward compatibility. Remove after retryIntervalMilliseconds is removed.
   * @return RetryInterval Duration
   */
  public final Duration getRetryInterval() {
    if (retryInterval == null) {
      if (retryIntervalMilliseconds != null) {
        retryInterval = Duration.ofMillis(retryIntervalMilliseconds);
      } else {
        retryInterval = Duration.ofMillis(DEFAULT_RETRY_INTERVAL_MILLISECONDS);
      }
    }

    return retryInterval;
  }
}

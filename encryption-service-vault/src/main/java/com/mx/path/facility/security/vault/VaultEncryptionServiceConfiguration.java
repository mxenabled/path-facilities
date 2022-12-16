package com.mx.path.facility.security.vault;

import java.time.Duration;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class VaultEncryptionServiceConfiguration {

  private static final int DEFAULT_ENGINE_VERSION = 1;
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final int DEFAULT_RETRY_INTERVAL_MILLISECONDS = 200;
  private static final String DEFAULT_AUTHENTICATION = "APPROLE";
  private static final String DEFAULT_KEY_NAME = "vault_session";
  private static final int DEFAULT_NUM_KEYS_TO_KEEP_COUNT = 1;

  @ConfigurationField
  private boolean enabled = true;

  @ConfigurationField
  private int engineVersion = DEFAULT_ENGINE_VERSION;

  @ConfigurationField
  private String keyName = DEFAULT_KEY_NAME;

  @ConfigurationField
  private int maxRetries = DEFAULT_MAX_RETRIES;

  @ConfigurationField
  private int numKeysToKeep = DEFAULT_NUM_KEYS_TO_KEEP_COUNT;

  /**
   * @#deprecated use retryInterval
   */
  @Deprecated
  @ConfigurationField
  private Integer retryIntervalMilliseconds = null;

  @ConfigurationField
  private Duration retryInterval;

  @ConfigurationField
  private String uri = DEFAULT_URI;

  @ConfigurationField("app-role")
  private String appRole;

  @ConfigurationField
  private String secretId;

  @ConfigurationField("app-id")
  private String appId;

  @ConfigurationField("user-id")
  private String userId;

  @ConfigurationField
  private String token;

  @ConfigurationField
  private String authentication = DEFAULT_AUTHENTICATION;

  /**
   * Maintaining backward compatibility. Remove when retryIntervalMilliseconds is removed.
   * @return retryInterval as Duration
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

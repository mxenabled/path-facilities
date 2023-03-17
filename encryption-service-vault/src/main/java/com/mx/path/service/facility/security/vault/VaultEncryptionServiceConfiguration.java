package com.mx.path.service.facility.security.vault;

import java.time.Duration;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class VaultEncryptionServiceConfiguration {
  enum AuthenticationType {
    @Deprecated
    APPID,
    APPROLE,
    TOKEN
  }

  private static final int DEFAULT_ENGINE_VERSION = 1;
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(200);
  private static final AuthenticationType DEFAULT_AUTHENTICATION = AuthenticationType.APPROLE;
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

  @ConfigurationField
  private Duration retryInterval = DEFAULT_RETRY_INTERVAL;

  @ConfigurationField
  private String uri = DEFAULT_URI;

  @ConfigurationField("app-role")
  private String appRole;

  @ConfigurationField(secret = true)
  private String secretId;

  @ConfigurationField("app-id")
  private String appId;

  @ConfigurationField("user-id")
  private String userId;

  @ConfigurationField(secret = true)
  private String token;

  @ConfigurationField
  private AuthenticationType authentication = DEFAULT_AUTHENTICATION;
}

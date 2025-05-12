package com.mx.path.service.facility.store.vault;

import java.time.Duration;

import lombok.Data;

import com.mx.path.core.common.configuration.ConfigurationField;

@Data
public class VaultStoreConfiguration {

  enum AuthenticationType {
    @Deprecated
    APPID,
    APPROLE,
    TOKEN
  }

  private static final AuthenticationType DEFAULT_AUTHENTICATION = AuthenticationType.APPROLE;
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final String PUT_IF_NOT_EXIST_UNSUPPORTED = "Put if not exist operations are not supported with Vault";
  private static final String SET_UNSUPPORTED = "Sets are not supported with Vault";
  private static final String TTL_UNSUPPORTED = "TTL is not supported with Vault";
  private static final int DEFAULT_ENGINE_VERSION = 2;
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(200);
  private static final int KEY_NOT_FOUND = 404;
  private static final int MAXIMUM_REAUTHENTICATION_RETRIES = 3;
  private static final boolean DEFAULT_SSL_ENABLED = false;

  @ConfigurationField(value = "app-id")
  private String appId;

  @ConfigurationField(value = "app-role")
  private String appRole;

  @ConfigurationField
  private AuthenticationType authentication = DEFAULT_AUTHENTICATION;

  @ConfigurationField
  private int engineVersion = DEFAULT_ENGINE_VERSION;

  @ConfigurationField
  private int maxRetries = DEFAULT_MAX_RETRIES;

  @ConfigurationField
  private Duration retryInterval = DEFAULT_RETRY_INTERVAL;

  @ConfigurationField(secret = true)
  private String secretId;

  @ConfigurationField(value = "ssl-enabled")
  private boolean ssl = DEFAULT_SSL_ENABLED;

  @ConfigurationField(secret = true)
  private String token;

  @ConfigurationField
  private String uri = DEFAULT_URI;

  @ConfigurationField(value = "user-id")
  private String userId;
}

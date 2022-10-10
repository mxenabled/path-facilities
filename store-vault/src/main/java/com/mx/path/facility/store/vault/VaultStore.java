package com.mx.path.facility.store.vault;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.mx.common.collections.ObjectMap;
import com.mx.common.lang.Strings;
import com.mx.common.store.Store;

public final class VaultStore implements Store {
  private static final int DEFAULT_ENGINE_VERSION = 2;
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final int DEFAULT_RETRY_INTERVAL_MILLISECONDS = 200;
  private static final int KEY_NOT_FOUND = 404;
  private static final String DEFAULT_AUTHENTICATION = "APPID";
  private static final String TTL_UNSUPPORTED = "TTL is not supported with Vault";
  private static final String SET_UNSUPPORTED = "Sets are not supported with Vault";
  private static final String PUT_IF_NOT_EXIST_UNSUPPORTED = "Put if not exist operations are not supported with Vault";
  private static final int MAXIMUM_REAUTHENTICATION_RETRIES = 3;

  @Getter
  private final ObjectMap configurations;
  @Setter
  private volatile Vault driver;

  enum AuthenticationType {
    APPID,
    APPROLE,
    TOKEN
  }

  public VaultStore(ObjectMap configurations) {
    this.configurations = configurations;
  }

  /**
   * Builds and configures instance of Vault
   *
   * @param authToken
   * @return configured Vault driver
   */
  Vault buildVaultDriver(@Nullable String authToken) {
    try {
      VaultConfig vaultConfig = new VaultConfig()
          .token(authToken)
          .engineVersion(getConfigurations().getAsInteger("engineVersion", DEFAULT_ENGINE_VERSION))
          .address(getConfigurations().getAsString("uri", DEFAULT_URI))
          .build();

      Vault newDriver = new Vault(vaultConfig);

      if (getConfigurations().getAsInteger("maxRetries", DEFAULT_MAX_RETRIES) > 0) {
        newDriver.withRetries(
            getConfigurations().getAsInteger("maxRetries", DEFAULT_MAX_RETRIES),
            getConfigurations().getAsInteger("retryIntervalMilliseconds", DEFAULT_RETRY_INTERVAL_MILLISECONDS));
      }

      return newDriver;
    } catch (Exception e) {
      throw new VaultStoreConfigurationException("Unable to build Vault Configuration", e);
    }
  }

  /**
   * Gets a token using given driver then builds and returns a new, authenticated driver
   *
   * @param newDriver that can be used to get an auth token
   * @return new driver with auth token
   */
  Vault authenticateDriver(Vault newDriver) {
    try {
      String token;
      if (getAuthenticationType().equals(AuthenticationType.TOKEN)) {
        token = getConfigurations().getAsString("token");

        if (token == null) {
          throw new VaultStoreConfigurationException("Vault token required for TOKEN authentication");
        }
      } else if (getAuthenticationType().equals(AuthenticationType.APPROLE)) {
        AuthResponse resp = newDriver.auth().loginByAppRole(configurations.getAsString("app-role"), getConfigurations().getAsString("secretId"));
        validateVaultAuthenticationResponse(resp, "Unable to login via app-role");

        token = resp.getAuthClientToken();
      } else {
        AuthResponse resp = newDriver.auth().loginByAppID("app-id/login", getConfigurations().getAsString("app-id"), getConfigurations().getAsString("user-id"));
        validateVaultAuthenticationResponse(resp, "Unable to login via app-id");

        token = resp.getAuthClientToken();
      }

      return buildVaultDriver(token);
    } catch (VaultException e) {
      throw new VaultStoreAuthenticationException("Unable to authenticate", e);
    }
  }

  private Vault getDriver() {
    if (driver == null) {
      synchronized (this) {
        if (driver == null) {
          Vault newDriver = buildVaultDriver(null);
          driver = authenticateDriver(newDriver);
        }
      }
    }

    return driver;
  }

  private AuthenticationType getAuthenticationType() {
    return AuthenticationType.valueOf(getConfigurations().getAsString("authentication", DEFAULT_AUTHENTICATION));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateVaultAuthenticationResponse(VaultResponse response, String errorMessage) {
    if (response != null && response.getRestResponse() != null && (response.getRestResponse().getStatus() < 200 || response.getRestResponse().getStatus() >= 300)) {
      throw new VaultStoreAuthenticationException(errorMessage + " (" + response.getRestResponse().getStatus() + ")");
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateVaultOperationResponse(VaultResponse response, String errorMessage) {
    if (response != null && response.getRestResponse() != null && (response.getRestResponse().getStatus() < 200 || response.getRestResponse().getStatus() >= 300)) {
      throw new VaultStoreOperationException(errorMessage + " (" + response.getRestResponse().getStatus() + ")", response.getRestResponse().getStatus());
    }
  }

  String encodeBase64(String value) {
    if (Strings.isNotBlank(value)) {
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    return value;
  }

  String decodeBase64(String value) {
    if (Strings.isNotBlank(value)) {
      return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    return value;
  }

  @Override
  public void delete(String key) {
    String path = "secret/" + key;
    LogicalResponse response = logicalDeleteWithReauthentication(path);
    validateVaultOperationResponse(response, "Delete failed");
  }

  @Override
  public void deleteSet(String key, String value) {
    throw new VaultStoreUnsupportedOperation(SET_UNSUPPORTED);
  }

  @Override
  public String get(String key) {
    String path = "secret/" + key;

    try {
      LogicalResponse response = logicalReadWithReauthentication(path);
      validateVaultOperationResponse(response, "Get failed");

      return decodeBase64(response.getData().get("value"));
    } catch (VaultStoreOperationException e) {
      if (e.getHttpStatusCode() == KEY_NOT_FOUND) {
        return null;
      }
      throw e;
    }
  }

  @Override
  public Set<String> getSet(String key) {
    throw new VaultStoreUnsupportedOperation(SET_UNSUPPORTED);
  }

  @Override
  public boolean inSet(String key, String value) {
    throw new VaultStoreUnsupportedOperation(SET_UNSUPPORTED);
  }

  @Override
  public void put(String key, String value) {
    String encodedValue = encodeBase64(value);
    String path = "secret/" + key;

    LogicalResponse response = logicalWriteWithReauthentication(path, Collections.singletonMap("value", encodedValue));
    validateVaultOperationResponse(response, "Put failed");
  }

  @Override
  public void put(String key, String value, long expirySeconds) {
    throw new VaultStoreUnsupportedOperation(TTL_UNSUPPORTED);
  }

  @Override
  public void putSet(String key, String value, long expirySeconds) {
    throw new VaultStoreUnsupportedOperation(SET_UNSUPPORTED);
  }

  @Override
  public void putSet(String key, String value) {
    throw new VaultStoreUnsupportedOperation(SET_UNSUPPORTED);
  }

  @Override
  public boolean putIfNotExist(String key, String value, long expirySeconds) {
    throw new VaultStoreUnsupportedOperation(PUT_IF_NOT_EXIST_UNSUPPORTED);
  }

  @Override
  public boolean putIfNotExist(String key, String value) {
    throw new VaultStoreUnsupportedOperation(PUT_IF_NOT_EXIST_UNSUPPORTED);
  }

  private void reauthenticateDriver() {
    authenticateDriver(getDriver());
  }

  private LogicalResponse logicalReadWithReauthentication(final String path) {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      try {
        return getDriver().logical().read(path);
      } catch (VaultException e) {
        // If we get a permission denied error, we should attempt to reauthenticate and retry.
        if (e.getMessage().contains("permission denied")) {
          reauthenticateDriver();
          continue;
        }

        throw new VaultStoreOperationException("Logical read failed", e);
      }
    }

    throw new VaultStoreAuthenticationException("Logical read permission denied and unable to reauthenticate");
  }

  private LogicalResponse logicalWriteWithReauthentication(final String path, final Map<String, Object> nameValuePairs) {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      try {
        return getDriver().logical().write(path, nameValuePairs);
      } catch (VaultException e) {
        // If we get a permission denied error, we should attempt to reauthenticate and retry.
        if (e.getMessage().contains("permission denied")) {
          reauthenticateDriver();
          continue;
        }

        throw new VaultStoreOperationException("Logical write failed", e);
      }
    }

    throw new VaultStoreAuthenticationException("Logical write permission denied and unable to reauthenticate");
  }

  private LogicalResponse logicalDeleteWithReauthentication(final String path) {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      try {
        return getDriver().logical().delete(path);
      } catch (VaultException e) {
        // If we get a permission denied error, we should attempt to reauthenticate and retry.
        if (e.getMessage().contains("permission denied")) {
          reauthenticateDriver();
          continue;
        }

        throw new VaultStoreOperationException("Logical delete failed", e);
      }
    }

    throw new VaultStoreAuthenticationException("Logical delete permission denied and unable to reauthenticate");
  }
}

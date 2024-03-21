package com.mx.path.service.facility.security.vault;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.lang.Strings;
import com.mx.path.core.common.security.EncryptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultEncryptionService implements EncryptionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(VaultEncryptionService.class);
  private static final int MAXIMUM_REAUTHENTICATION_RETRIES = 3;
  private static final int PERMISSION_DENIED_STATUS = 403;

  private static boolean initialized = false;

  @Getter
  private final VaultEncryptionServiceConfiguration configuration;

  @Setter
  private volatile Vault driver;

  public VaultEncryptionService(@Configuration VaultEncryptionServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Encrypts given plaintext. Blank and null are returned unaffected
   *
   * <p>Rebuilds the driver on failure and tries again.
   *
   * @param plaintext unencrypted text
   * @return ciphertext encrypted text
   */
  @Override
  public final String encrypt(String plaintext) {
    initialize();

    if (!configuration.isEnabled() || Strings.isBlank(plaintext)) {
      return plaintext;
    }

    return doEncrypt(plaintext);
  }

  /**
   * Decrypt given ciphertext
   *
   * <p>Rebuilds the driver on failure and tries again.
   *
   * @param ciphertext encrypted text
   * @return plaintext unencrypted text
   */
  @Override
  public final String decrypt(String ciphertext) {
    initialize();

    if (!configuration.isEnabled() || Strings.isBlank(ciphertext)) {
      return ciphertext;
    }

    return doDecrypt(ciphertext);
  }

  /**
   * Determines if given text is a valid ciphertext
   *
   * @param value to evaluate
   * @return true if given value is encrypted by this service, otherwise false
   */
  @Override
  public final boolean isEncrypted(String value) {
    if (Objects.nonNull(value)) {
      return value.startsWith("vault");
    }

    return false;
  }

  /**
   * Rotate transit key
   *
   * <p>Does not raise exception on failure.
   */
  @Override
  public final void rotateKeys() {
    try {
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configuration.getKeyName() + "/rotate", null);
      validateVaultOperationResponse(response, "Unable to rotate vault key");
      LOGGER.info("Rotated vault key: " + configuration.getKeyName());
    } catch (RuntimeException e) {
      LOGGER.warn("Unable to rotate vault key", e);
    }

    VaultTransitKey key = loadKey();

    if (Objects.isNull(key)) {
      return;
    }

    LOGGER.info("rotateKeys.currentKeyVersion = " + key.currentKeyVersion());
    LOGGER.info("rotateKeys.numKeysToKeep = " + configuration.getNumKeysToKeep());
    LOGGER.info("rotateKeys.minDecryptVersion = " + (key.currentKeyVersion() - configuration.getNumKeysToKeep()));
    int minDecryptVersion = key.currentKeyVersion() - configuration.getNumKeysToKeep();

    if (minDecryptVersion < 1) {
      minDecryptVersion = 1;
    }

    setMinDecryptionVersion(minDecryptVersion);
  }

  /**
   * Builds and configures instance of Vault
   *
   * @param authToken auth token for driver
   * @return configured Vault driver
   */
  final Vault buildVaultDriver(@Nullable String authToken) {
    try {
      VaultConfig vaultConfig = new VaultConfig()
          .token(authToken)
          .engineVersion(configuration.getEngineVersion())
          .address(configuration.getUri())
          .build();

      Vault newDriver = new Vault(vaultConfig);

      if (configuration.getMaxRetries() > 0) {
        newDriver.withRetries(
            configuration.getMaxRetries(),
            Math.toIntExact(configuration.getRetryInterval().toMillis()));
      }

      return newDriver;
    } catch (VaultException e) {
      throw new VaultEncryptionConfigurationException("Unable to build Vault Encryption Configuration", e);
    }
  }

  /**
   * Create transit key
   *
   * <p>Does not raise exception on failure.
   */
  final void initKey() {
    try {
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configuration.getKeyName(), Collections.singletonMap("exportable", "true"));
      validateVaultOperationResponse(response, "Unable to initialize vault key");
    } catch (RuntimeException e) {
      LOGGER.warn("Unable to initialize new vault key. Assuming that the key already exists", e);
    }
  }

  /**
   * Load key configuration
   *
   * <p>Does not raise exceptions
   *
   * @return the key, if successful, else null
   */
  final VaultTransitKey loadKey() {
    try {
      LogicalResponse response = logicalReadWithReauthentication("transit/keys/" + configuration.getKeyName());
      validateVaultOperationResponse(response, "Key read failed");

      JsonObject j = response.getDataObject();

      return VaultTransitKey.fromJsonObject(j);
    } catch (RuntimeException e) {
      LOGGER.warn("Unable to read vault key", e);
      return null;
    }
  }

  /**
   * Set the minimum decryption key
   *
   * <p>Does not raise exception on failure.
   *
   * @param minDecryptionVersion
   */
  final void setMinDecryptionVersion(int minDecryptionVersion) {
    try {
      //FIXME should `min_available_version` and `min_decyprtion_version` always be the same and `min_encryption_version` be ahead?
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configuration.getKeyName() + "/config", Collections.singletonMap("min_decryption_version", minDecryptionVersion));

      //      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configuration.getKeyName() + "/config", ImmutableMap.of(
      //          "min_decryption_version", minDecryptionVersion,
      //          "min_encryption_version", minDecryptionVersion,
      //          "min_available_version", minDecryptionVersion));
      validateVaultOperationResponse(response, "Unable to update vault key");
    } catch (RuntimeException e) {
      LOGGER.warn("Unable to update vault key", e);
    }
  }

  /**
   * Gets a token using given driver then builds and returns a new, authenticated driver
   *
   * @param authenticationDriver that can be used to get an auth token
   * @return new driver with auth token
   */
  final Vault buildAuthenticatedDriver(Vault authenticationDriver) {
    try {
      String token;
      AuthResponse resp;

      switch (getConfiguration().getAuthentication()) {
        case TOKEN:
          token = configuration.getToken();

          if (token == null) {
            throw new VaultEncryptionConfigurationException("Vault token required for TOKEN authentication");
          }
          break;

        case APPROLE:
          resp = authenticationDriver.auth().loginByAppRole(configuration.getAppRole(), configuration.getSecretId());
          validateVaultAuthenticationResponse(resp, "Unable to login via vault app-role");

          token = resp.getAuthClientToken();
          break;

        case APPID:
          resp = authenticationDriver.auth().loginByAppID("app-id/login", configuration.getAppId(), configuration.getUserId());
          validateVaultAuthenticationResponse(resp, "Unable to login via vault app-id");

          token = resp.getAuthClientToken();
          break;

        default:
          throw new VaultEncryptionConfigurationException("Invalid vault authentication type: " + getConfiguration().getAuthentication());
      }

      return buildVaultDriver(token);
    } catch (VaultException e) {
      throw new VaultEncryptionAuthenticationException("Unable to login to vault", e);
    }
  }

  /**
   * Resets the driver. Calling {@link #getDriver()} will create a new, authenticated driver.
   */
  final void resetDriver() {
    this.driver = null;
  }

  private String doEncrypt(String plaintext) {
    String encodedValue = encodeBase64(plaintext);
    String path = "transit/encrypt/" + configuration.getKeyName();

    LogicalResponse response = logicalWriteWithReauthentication(path, Collections.singletonMap("plaintext", encodedValue));
    validateVaultOperationResponse(response, "Vault encrypt failed");

    return response.getData().get("ciphertext");
  }

  private String doDecrypt(String ciphertext) {
    LogicalResponse response = logicalWriteWithReauthentication(
        "transit/decrypt/" + configuration.getKeyName(),
        Collections.singletonMap("ciphertext", ciphertext));
    validateVaultOperationResponse(response, "Vault decrypt failed");

    String plaintext = response.getData().get("plaintext");

    return decodeBase64(plaintext);
  }

  private Vault getDriver() {
    if (driver == null) {
      synchronized (this) {
        if (driver == null) {
          Vault authenticationDriver = buildVaultDriver(null); // This is used to authenticate only
          driver = buildAuthenticatedDriver(authenticationDriver);
        }
      }
    }

    return driver;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateVaultAuthenticationResponse(VaultResponse response, String errorMessage) {
    if (response != null && response.getRestResponse() != null && (response.getRestResponse().getStatus() < 200 || response.getRestResponse().getStatus() >= 300)) {
      throw new VaultEncryptionOperationException(errorMessage + " (" + response.getRestResponse().getStatus() + ")");
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateVaultOperationResponse(VaultResponse response, String errorMessage) {
    if (response != null && response.getRestResponse() != null && (response.getRestResponse().getStatus() < 200 || response.getRestResponse().getStatus() >= 300)) {
      throw new VaultEncryptionOperationException(errorMessage + " (" + response.getRestResponse().getStatus() + ")");
    }
  }

  private String encodeBase64(String value) {
    if (Strings.isNotBlank(value)) {
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    return value;
  }

  private String decodeBase64(String value) {
    if (Strings.isNotBlank(value)) {
      return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    return value;
  }

  private void initialize() {
    if (initialized) {
      return;
    }
    initKey();
    initialized = true;
  }

  private LogicalResponse logicalReadWithReauthentication(final String path) {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      LogicalResponse response = null;
      try {
        response = getDriver().logical().read(path);
      } catch (VaultEncryptionAuthenticationException e) {
        resetDriver();
        continue;
      } catch (VaultException e) {
        throw new VaultEncryptionOperationException("Vault logical read failed", e);
      }

      // If the response is permission denied
      if (response != null && response.getRestResponse() != null && response.getRestResponse().getStatus() == PERMISSION_DENIED_STATUS) {
        resetDriver();
      } else {
        return response;
      }
    }

    throw new VaultEncryptionAuthenticationException("Permission denied and unable to reauthenticate vault read");
  }

  private LogicalResponse logicalWriteWithReauthentication(final String path, final Map<String, Object> nameValuePairs) {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      LogicalResponse response = null;
      try {
        response = getDriver().logical().write(path, nameValuePairs);
      } catch (VaultEncryptionAuthenticationException e) {
        resetDriver();
        continue;
      } catch (VaultException e) {
        throw new VaultEncryptionOperationException("Vault logical write failed", e);
      }

      // If the response is permission denied
      if (response != null && response.getRestResponse() != null && response.getRestResponse().getStatus() == PERMISSION_DENIED_STATUS) {
        resetDriver();
      } else {
        return response;
      }
    }

    throw new VaultEncryptionAuthenticationException("Permission denied and unable to reauthenticate vault write");
  }
}

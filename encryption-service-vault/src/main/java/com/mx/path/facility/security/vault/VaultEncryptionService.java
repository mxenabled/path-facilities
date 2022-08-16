package com.mx.path.facility.security.vault;

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
import com.mx.common.collections.ObjectMap;
import com.mx.common.lang.Strings;
import com.mx.common.security.EncryptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultEncryptionService implements EncryptionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(VaultEncryptionService.class);
  private static final int DEFAULT_ENGINE_VERSION = 1;
  private static final String DEFAULT_URI = "http://127.0.0.1:8200";
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final int DEFAULT_RETRY_INTERVAL_MILLISECONDS = 200;
  private static final String DEFAULT_AUTHENTICATION = "APPID";
  private static final String DEFAULT_KEY_NAME = "vault_session";
  private static final int DEFAULT_NUM_KEYS_TO_KEEP_COUNT = 1;
  private static final int MAXIMUM_REAUTHENTICATION_RETRIES = 3;
  private static final int PERMISSION_DENIED_STATUS = 403;

  private static boolean initialized = false;

  @Getter
  private final ObjectMap configurations;
  @Setter
  private volatile Vault driver;

  enum AuthenticationType {
    APPID,
    APPROLE,
    TOKEN
  }

  public VaultEncryptionService(ObjectMap configurations) {
    this.configurations = configurations;
  }

  /**
   * Encrypts given plaintext. Blank and null are returned unaffected
   *
   * Rebuilds the driver on failure and tries again.
   *
   * @param plaintext
   * @return ciphertext
   */
  @Override
  public final String encrypt(String plaintext) {
    initialize();

    if (!configurations.getAsBoolean("enabled", false) || Strings.isBlank(plaintext)) {
      return plaintext;
    }

    return doEncrypt(plaintext);
  }

  /**
   * Decrypt given ciphertext
   *
   * Rebuilds the driver on failure and tries again.
   *
   * @param ciphertext
   * @return plaintext
   */
  @Override
  public final String decrypt(String ciphertext) {
    initialize();

    if (!configurations.getAsBoolean("enabled", false) || Strings.isBlank(ciphertext)) {
      return ciphertext;
    }

    return doDecrypt(ciphertext);
  }

  /**
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
   * Does not raise exception on failure.
   */
  @Override
  public final void rotateKeys() {
    try {
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME) + "/rotate", null);
      validateVaultResponse(response, "Unable to rotate vault key");
      LOGGER.info("Rotated vault key: " + configurations.getAsString("keyName", DEFAULT_KEY_NAME));
    } catch (VaultException e) {
      LOGGER.warn("Unable to rotate vault key", e);
    }

    VaultTransitKey key = loadKey();

    if (Objects.isNull(key)) {
      return;
    }

    int minDecryptVersion = key.currentKeyVersion() - configurations.getAsInteger("numKeysToKeep", DEFAULT_NUM_KEYS_TO_KEEP_COUNT);

    if (minDecryptVersion < 1) {
      minDecryptVersion = 1;
    }

    setMinDecryptionVersion(minDecryptVersion);
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
          .engineVersion(configurations.getAsInteger("engineVersion", DEFAULT_ENGINE_VERSION))
          .address(configurations.getAsString("uri", DEFAULT_URI))
          .build();

      Vault newDriver = new Vault(vaultConfig);

      if (configurations.getAsInteger("maxRetries", DEFAULT_MAX_RETRIES) > 0) {
        newDriver.withRetries(
            configurations.getAsInteger("maxRetries", DEFAULT_MAX_RETRIES),
            configurations.getAsInteger("retryIntervalMilliseconds", DEFAULT_RETRY_INTERVAL_MILLISECONDS));
      }

      return newDriver;
    } catch (VaultException e) {
      throw new RuntimeException("Unable to build Vault Configuration", e);
    }
  }

  /**
   * Create transit key
   *
   * Does not raise exception on failure.
   */
  final void initKey() {
    try {
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME), Collections.singletonMap("exportable", "true"));
      validateVaultResponse(response, "Unable to initialize vault key");
    } catch (VaultException e) {
      LOGGER.warn("Unable to initialize new vault key. Assuming that the key already exists", e);
    }
  }

  /**
   * Load key configuration
   *
   * @return the key, if successful, else null
   */
  final VaultTransitKey loadKey() {
    try {
      LogicalResponse response = logicalReadWithReauthentication("transit/keys/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME));
      validateVaultResponse(response, "Key read failed");

      JsonObject j = response.getDataObject();

      return VaultTransitKey.fromJsonObject(j);
    } catch (VaultException e) {
      LOGGER.warn("Unable to read vault key", e);
    }

    return null;
  }

  /**
   * Set the minimum decryption key
   *
   * Does not raise exception on failure.
   *
   * @param minDecryptionVersion
   */
  final void setMinDecryptionVersion(int minDecryptionVersion) {
    try {
      VaultResponse response = logicalWriteWithReauthentication("transit/keys/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME), Collections.singletonMap("min_decryption_version", minDecryptionVersion));
      validateVaultResponse(response, "Unable to update vault key");
    } catch (VaultException e) {
      LOGGER.warn("Unable to update vault key", e);
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
        token = configurations.getAsString("token");

        if (token == null) {
          throw new RuntimeException("Vault token required for TOKEN authentication");
        }
      } else if (getAuthenticationType().equals(AuthenticationType.APPROLE)) {
        AuthResponse resp = newDriver.auth().loginByAppRole(configurations.getAsString("app-role"), configurations.getAsString("secretId"));
        validateVaultResponse(resp, "Unable to login via jwt");

        token = resp.getAuthClientToken();
      } else {
        AuthResponse resp = newDriver.auth().loginByAppID("app-id/login", configurations.getAsString("app-id"), configurations.getAsString("user-id"));
        validateVaultResponse(resp, "Unable to login via app-id");

        token = resp.getAuthClientToken();
      }

      return buildVaultDriver(token);
    } catch (VaultException e) {
      throw new RuntimeException("Unable to login to vault", e);
    }
  }

  private String doEncrypt(String plaintext) {
    try {
      String encodedValue = encodeBase64(plaintext);
      String path = "transit/encrypt/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME);

      LogicalResponse response = logicalWriteWithReauthentication(path, Collections.singletonMap("plaintext", encodedValue));
      validateVaultResponse(response, "Encrypt failed");

      return response.getData().get("ciphertext");
    } catch (VaultException e) {
      throw new RuntimeException("Unable to encrypt value", e);
    }
  }

  private String doDecrypt(String ciphertext) {
    try {
      LogicalResponse response = logicalWriteWithReauthentication(
          "transit/decrypt/" + configurations.getAsString("keyName", DEFAULT_KEY_NAME),
          Collections.singletonMap("ciphertext", ciphertext));
      validateVaultResponse(response, "Decrypt failed");

      String plaintext = response.getData().get("plaintext");

      return decodeBase64(plaintext);
    } catch (VaultException e) {
      throw new RuntimeException("Unable to decrypt value", e);
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

  /**
   * reauthenticat to vault using the configured driver
   */
  void reauthenticateDriver() {
    driver = null;
    getDriver();
  }

  private AuthenticationType getAuthenticationType() {
    return AuthenticationType.valueOf(configurations.getAsString("authentication", DEFAULT_AUTHENTICATION));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateVaultResponse(VaultResponse response, String errorMessage) throws VaultException {
    if (response != null && response.getRestResponse() != null && (response.getRestResponse().getStatus() < 200 || response.getRestResponse().getStatus() >= 300)) {
      throw new VaultException(errorMessage + " (" + response.getRestResponse().getStatus() + ")");
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

  private LogicalResponse logicalReadWithReauthentication(final String path) throws VaultException {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      LogicalResponse response = getDriver().logical().read(path);

      if (response != null && response.getRestResponse() != null && response.getRestResponse().getStatus() == PERMISSION_DENIED_STATUS) {
        reauthenticateDriver();
      } else {
        return response;
      }
    }

    throw new VaultException("permission denied and unable to reauthenticate");
  }

  private LogicalResponse logicalWriteWithReauthentication(final String path, final Map<String, Object> nameValuePairs) throws VaultException {
    for (int i = 0; i < MAXIMUM_REAUTHENTICATION_RETRIES; i++) {
      LogicalResponse response = getDriver().logical().write(path, nameValuePairs);

      if (response != null && response.getRestResponse() != null && response.getRestResponse().getStatus() == PERMISSION_DENIED_STATUS) {
        reauthenticateDriver();
      } else {
        return response;
      }
    }

    throw new VaultException("permission denied and unable to reauthenticate");
  }
}

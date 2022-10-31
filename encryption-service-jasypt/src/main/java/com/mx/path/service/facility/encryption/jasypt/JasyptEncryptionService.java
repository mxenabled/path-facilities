package com.mx.path.service.facility.encryption.jasypt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import com.mx.common.collections.ObjectArray;
import com.mx.common.collections.ObjectMap;
import com.mx.common.security.EncryptionService;

import org.apache.commons.codec.digest.DigestUtils;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

/**
 * <h1>Jasypt EncryptionService</h1>
 *
 * <p>
 * Provides encryption service using the Jasypt library. Allows for multiple
 * keys and easy key rotation. Uses key at currentKeyIndex for all encryption and
 * decrypts using the original key used to encrypt (hashed identifier encoded in the cyphertext)
 *
 * <p>
 * Uses thread-safe, {@link PooledPBEStringEncryptor}
 *
 * <p>
 * To configure:
 *
 * <h2>In gateway.yml</h2>
 * {@code
 * encryptionService:
 *   class: com.mx.web.security.JasyptEncryptionService
 *   configurations:
 *     poolSize: 10
 *     currentKeyIndex: 0
 *     keys:
 *     - 6251655468576D5A7134743777217A24
 *     - 442A472D4B6150645367566B59703373
 * }
 *
 * <p>
 * To rotate keys, add to the keys list.
 * Change the currentKeyIndex to the zero-based index of key to use for all encryption
 */
public class JasyptEncryptionService implements EncryptionService {

  private static final boolean DEFAULT_ENABLED = false;
  private static final int DEFAULT_POOL_SIZE = 10;

  // Fields

  @Getter
  private final ObjectMap configurations;
  @Setter(AccessLevel.PACKAGE)
  private PooledPBEStringEncryptor currentEncryptor;
  private String currentKeyIdentifier;
  @Getter(AccessLevel.PACKAGE)
  private final Map<String, PooledPBEStringEncryptor> encryptors = new HashMap<String, PooledPBEStringEncryptor>();

  // Constructors

  public JasyptEncryptionService(ObjectMap configurations) {
    this.configurations = configurations;

    ObjectArray keys = this.configurations.getArray("keys");
    keys.forEach(k -> addEncryptorForKey((String) k));
    if (keys.size() > 0) {
      currentKeyIdentifier = DigestUtils.md5Hex(keys.getAsString(configurations.getAsInteger("currentKeyIndex")));
      currentEncryptor = encryptors.get(currentKeyIdentifier);
    }
  }

  // Public

  @Override
  public final String decrypt(String cypher) {
    try {
      if (!isEncrypted(cypher)) {
        return cypher;
      }

      String[] parts = cypher.split(":");
      String key = parts[1];
      cypher = parts[2];
      PooledPBEStringEncryptor encryptor = encryptors.get(key);

      if (Objects.isNull(encryptor)) {
        throw new JasyptEncryptionOperationException("No suitable decryption key found");
      }

      return encryptor.decrypt(cypher);
    } catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e) {
      throw new JasyptEncryptionOperationException("Decrypt operation failed", e);
    }
  }

  @Override
  public final String encrypt(String value) {
    try {
      if (!configurations.getAsBoolean("enabled", DEFAULT_ENABLED)) {
        return value;
      }

      String cypher = currentEncryptor.encrypt(value);
      cypher = "jasypt:" + currentKeyIdentifier + ":" + cypher;

      return cypher;
    } catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e) {
      throw new JasyptEncryptionOperationException("Encrypt operation failed", e);
    }
  }

  @Override
  public final boolean isEncrypted(String value) {
    if (Objects.isNull(value) || !configurations.getAsBoolean("enabled", DEFAULT_ENABLED)) {
      return false;
    }

    return value.startsWith("jasypt:");
  }

  // Private

  private String addEncryptorForKey(String key) {
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    config.setPassword(key);
    config.setAlgorithm("PBEWithMD5AndTripleDES");
    config.setKeyObtentionIterations("1000");
    config.setPoolSize(configurations.getAsInteger("poolSize", DEFAULT_POOL_SIZE));
    config.setProviderName("SunJCE");
    config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
    config.setStringOutputType("base64");

    PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
    encryptor.setConfig(config);

    String keyIdentifier = DigestUtils.md5Hex(key);
    encryptors.put(keyIdentifier, encryptor);

    return keyIdentifier;
  }
}

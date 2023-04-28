package com.mx.path.service.facility.encryption.jasypt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.security.EncryptionService;

import org.apache.commons.codec.digest.DigestUtils;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

/**
 * Jasypt EncryptionService
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
 * To rotate keys, add to the top of the keys list.
 * Change the currentKeyIndex to the zero-based index of key to use for all encryption
 */
public class JasyptEncryptionService implements EncryptionService {

  @Getter
  private final JasyptEncryptionServiceConfiguration configuration;
  @Setter(AccessLevel.PACKAGE)
  private PooledPBEStringEncryptor currentEncryptor;
  private String currentKeyIdentifier;
  @Getter(AccessLevel.PACKAGE)
  private final Map<String, PooledPBEStringEncryptor> encryptors = new HashMap<String, PooledPBEStringEncryptor>();

  // Constructors

  public JasyptEncryptionService(@Configuration JasyptEncryptionServiceConfiguration configuration) {
    this.configuration = configuration;

    List<String> keys = this.configuration.getKeys();
    keys.forEach(k -> addEncryptorForKey((String) k));
    if (keys.size() > 0) {
      currentKeyIdentifier = DigestUtils.md5Hex(keys.get(configuration.getCurrentKeyIndex()));
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
      if (!configuration.isEnabled()) {
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
    if (Objects.isNull(value) || !configuration.isEnabled()) {
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
    config.setPoolSize(configuration.getPoolSize());
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

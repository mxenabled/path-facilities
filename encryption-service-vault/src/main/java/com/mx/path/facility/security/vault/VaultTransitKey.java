package com.mx.path.facility.security.vault;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

import com.bettercloud.vault.json.JsonObject;

/**
 * Represents the current state of a vault transit key. See vault REST API documentation for field explanations.
 */
@Builder
public class VaultTransitKey {

  @Getter
  private boolean allowPlaintextBackup;

  @Getter
  private boolean deletionAllowed;

  @Getter
  private boolean derived;

  @Getter
  private boolean exportable;

  @Getter
  private Map<String, Integer> keys;

  @Getter
  private int minDecryptionVersion;

  @Getter
  private int minEncryptionVersion;

  @Getter
  private String name;

  @Getter
  private boolean supportsDecryption;

  @Getter
  private boolean supportsDerivation;

  @Getter
  private boolean supportsEncryption;

  @Getter
  private boolean supportsSigning;

  @Getter
  private String type;

  final int currentKeyVersion() {
    if (keys == null) {
      return 0;
    }

    return getKeys().keySet().stream().mapToInt(Integer::valueOf).max().orElse(0);
  }

  /**
   * Inflate new instance of VaultTransitKey from REST API JSON response body.
   *
   * @param json object
   * @return new VaultTransitKey instance
   */
  public static VaultTransitKey fromJsonObject(JsonObject json) {

    Map<String, Integer> keys = new LinkedHashMap<>();
    if (json.get("keys") != null) {
      JsonObject keyValues = json.get("keys").asObject();
      keyValues.names().forEach((n) -> {
        keys.put(n, keyValues.getInt(n));
      });
    }

    return builder()
        .allowPlaintextBackup(json.getBoolean("allow_plaintext_backup", false))
        .deletionAllowed(json.getBoolean("deletion_allowed", false))
        .derived(json.getBoolean("derived", false))
        .exportable(json.getBoolean("exportable", false))
        .keys(keys)
        .minDecryptionVersion(json.getInt("min_decryption_version", 0))
        .minEncryptionVersion(json.getInt("min_encryption_version", 0))
        .name(json.getString("name"))
        .supportsDecryption(json.getBoolean("supports_decryption", false))
        .supportsDerivation(json.getBoolean("supports_derivation", false))
        .supportsEncryption(json.getBoolean("supports_encryption", false))
        .supportsSigning(json.getBoolean("allow_plaintext_backup", false))
        .type(json.getString("type"))
        .build();
  }
}

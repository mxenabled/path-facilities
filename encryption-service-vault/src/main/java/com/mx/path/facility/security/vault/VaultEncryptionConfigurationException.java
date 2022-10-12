package com.mx.path.facility.security.vault;

import com.mx.common.facility.FacilityException;

public class VaultEncryptionConfigurationException extends FacilityException {
  public VaultEncryptionConfigurationException(String message) {
    super(message);
  }

  public VaultEncryptionConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}

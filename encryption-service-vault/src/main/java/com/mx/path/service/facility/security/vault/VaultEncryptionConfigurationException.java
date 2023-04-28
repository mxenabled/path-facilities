package com.mx.path.service.facility.security.vault;

import com.mx.path.core.common.facility.FacilityException;

public class VaultEncryptionConfigurationException extends FacilityException {
  public VaultEncryptionConfigurationException(String message) {
    super(message);
  }

  public VaultEncryptionConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}

package com.mx.path.service.facility.security.vault;

import com.mx.path.core.common.facility.FacilityException;

public class VaultEncryptionOperationException extends FacilityException {
  public VaultEncryptionOperationException(String message) {
    super(message);
  }

  public VaultEncryptionOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

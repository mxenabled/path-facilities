package com.mx.path.facility.security.vault;

import com.mx.common.facility.FacilityException;

public class VaultEncryptionOperationException extends FacilityException {
  public VaultEncryptionOperationException(String message) {
    super(message);
  }

  public VaultEncryptionOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

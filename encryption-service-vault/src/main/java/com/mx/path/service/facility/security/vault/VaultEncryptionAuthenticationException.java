package com.mx.path.service.facility.security.vault;

import com.mx.common.facility.FacilityException;

public class VaultEncryptionAuthenticationException extends FacilityException {
  public VaultEncryptionAuthenticationException(String message) {
    super(message);
  }

  public VaultEncryptionAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

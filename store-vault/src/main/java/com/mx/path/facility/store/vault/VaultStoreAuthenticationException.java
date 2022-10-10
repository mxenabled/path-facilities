package com.mx.path.facility.store.vault;

import com.mx.common.facility.FacilityException;

public class VaultStoreAuthenticationException extends FacilityException {
  public VaultStoreAuthenticationException(String message) {
    super(message);
  }

  public VaultStoreAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

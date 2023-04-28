package com.mx.path.service.facility.store.vault;

import com.mx.path.core.common.facility.FacilityException;

public class VaultStoreAuthenticationException extends FacilityException {
  public VaultStoreAuthenticationException(String message) {
    super(message);
  }

  public VaultStoreAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

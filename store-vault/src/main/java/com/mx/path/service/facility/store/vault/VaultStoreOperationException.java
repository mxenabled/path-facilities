package com.mx.path.service.facility.store.vault;

import lombok.Getter;

import com.mx.path.core.common.facility.FacilityException;

public class VaultStoreOperationException extends FacilityException {
  @Getter
  private int httpStatusCode;

  public VaultStoreOperationException(String message) {
    super(message);
  }

  public VaultStoreOperationException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public VaultStoreOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

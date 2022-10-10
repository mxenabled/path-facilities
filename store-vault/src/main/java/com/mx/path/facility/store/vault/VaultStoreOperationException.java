package com.mx.path.facility.store.vault;

import lombok.Getter;

import com.mx.common.facility.FacilityException;

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

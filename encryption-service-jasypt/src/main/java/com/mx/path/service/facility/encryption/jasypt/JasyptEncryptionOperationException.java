package com.mx.path.service.facility.encryption.jasypt;

import com.mx.path.core.common.facility.FacilityException;

public class JasyptEncryptionOperationException extends FacilityException {
  public JasyptEncryptionOperationException(String message) {
    super(message);
  }

  public JasyptEncryptionOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

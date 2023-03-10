package com.mx.path.service.facility.store.vault;

import com.mx.common.configuration.ConfigurationException;

public class VaultStoreConfigurationException extends ConfigurationException {
  public VaultStoreConfigurationException(String message) {
    super(message);
  }

  public VaultStoreConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}

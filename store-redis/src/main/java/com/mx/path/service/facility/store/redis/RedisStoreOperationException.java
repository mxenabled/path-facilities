package com.mx.path.service.facility.store.redis;

import com.mx.path.core.common.facility.FacilityException;

public class RedisStoreOperationException extends FacilityException {
  public RedisStoreOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

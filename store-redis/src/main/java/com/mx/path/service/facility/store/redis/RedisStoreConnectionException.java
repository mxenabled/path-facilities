package com.mx.path.service.facility.store.redis;

import com.mx.common.facility.FacilityException;

public class RedisStoreConnectionException extends FacilityException {
  public RedisStoreConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}

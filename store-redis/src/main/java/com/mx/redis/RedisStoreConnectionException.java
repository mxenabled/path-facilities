package com.mx.redis;

import com.mx.common.facility.FacilityException;

public class RedisStoreConnectionException extends FacilityException {
  public RedisStoreConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}

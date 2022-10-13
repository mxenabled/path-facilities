package com.mx.redis;

import com.mx.common.facility.FacilityException;

public class RedisStoreUnsupportedException extends FacilityException {
  public RedisStoreUnsupportedException(String message) {
    super(message);
  }
}

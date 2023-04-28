package com.mx.path.service.facility.store.redis;

import com.mx.path.core.common.facility.FacilityException;

public class RedisStoreUnsupportedException extends FacilityException {
  public RedisStoreUnsupportedException(String message) {
    super(message);
  }
}

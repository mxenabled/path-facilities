package com.mx.path.service.facility.encryption.jasypt;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import com.mx.common.configuration.ConfigurationField;

@Data
public class JasyptEncryptionServiceConfiguration {

  private static final boolean DEFAULT_ENABLED = true;
  private static final int DEFAULT_CURRENT_KEY_INDEX = 0;
  private static final int DEFAULT_POOL_SIZE = 10;

  @ConfigurationField
  private boolean enabled = DEFAULT_ENABLED;

  @ConfigurationField(placeholder = "0")
  private int currentKeyIndex = DEFAULT_CURRENT_KEY_INDEX;

  @ConfigurationField(required = true, elementType = String.class, placeholder = "9rXVaIYKFoEwZkY3eDPHneNVVLAAtNhDrF3ItjZDzNvYInSOROfg5WsWyf18AS6p0lNHzGqmGr1EeVWIKWKg3Xvr1f1wa9uVHHxUa3tcOUvZkaZYDyDX3pErXeXaMPyP")
  private List<String> keys = new ArrayList<>();

  @ConfigurationField(placeholder = "10")
  private int poolSize = DEFAULT_POOL_SIZE;
}

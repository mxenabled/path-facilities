package com.mx.path.service.facility.messaging.nats;

import com.mx.common.facility.FacilityException;

public class NatsMessageBrokerConfigurationException extends FacilityException {
  public NatsMessageBrokerConfigurationException(String message) {
    super(message);
  }

  public NatsMessageBrokerConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}

package com.mx.path.service.facility.messaging.nats;

import com.mx.path.core.common.messaging.MessageError;

public class NatsMessageBrokerOperationException extends MessageError {
  public NatsMessageBrokerOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

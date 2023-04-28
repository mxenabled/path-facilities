package com.mx.path.service.facility.messaging.nats;

import com.mx.path.core.common.messaging.MessageError;

public class NatsMessageBrokerMessageInterruptedException extends MessageError {
  public NatsMessageBrokerMessageInterruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}

package com.mx.messaging.nats;

import com.mx.common.messaging.MessageError;

public class NatsMessageBrokerOperationException extends MessageError {
  public NatsMessageBrokerOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}

package com.mx.messaging.nats;

import com.mx.common.messaging.MessageError;

public class NatsMessageBrokerMessageInterruptedException extends MessageError {
  public NatsMessageBrokerMessageInterruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}

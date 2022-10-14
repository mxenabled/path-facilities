package com.mx.messaging.nats;

import com.mx.common.messaging.MessageError;
import com.mx.common.messaging.MessageStatus;

public class NatsMessageBrokerDisabledException extends MessageError {
  public NatsMessageBrokerDisabledException(String message, Throwable cause) {
    super(message, MessageStatus.DISABLED, cause);
  }
}

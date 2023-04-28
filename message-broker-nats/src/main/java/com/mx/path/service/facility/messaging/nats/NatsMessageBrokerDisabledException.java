package com.mx.path.service.facility.messaging.nats;

import com.mx.path.core.common.messaging.MessageError;
import com.mx.path.core.common.messaging.MessageStatus;

public class NatsMessageBrokerDisabledException extends MessageError {
  public NatsMessageBrokerDisabledException(String message, Throwable cause) {
    super(message, MessageStatus.DISABLED, cause);
  }
}

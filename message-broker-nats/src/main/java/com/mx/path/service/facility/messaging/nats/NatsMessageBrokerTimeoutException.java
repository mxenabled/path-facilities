package com.mx.path.service.facility.messaging.nats;

import com.mx.path.core.common.messaging.MessageError;
import com.mx.path.core.common.messaging.MessageStatus;

public class NatsMessageBrokerTimeoutException extends MessageError {
  public NatsMessageBrokerTimeoutException(String message) {
    super(message, MessageStatus.TIMEOUT, null);
  }
}

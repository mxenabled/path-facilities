package com.mx.path.service.facility.messaging.nats;

import java.nio.charset.StandardCharsets;

import com.mx.common.messaging.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.client.Message;
import io.nats.client.MessageHandler;

public class NatsEventListener implements MessageHandler {
  private Logger logger = LoggerFactory.getLogger(NatsEventListener.class);
  private EventListener eventListener;

  public NatsEventListener(EventListener eventListener) {
    this.eventListener = eventListener;
  }

  @Override
  public final void onMessage(Message message) {
    String payload = new String(message.getData(), StandardCharsets.UTF_8);
    String channel = message.getSubject();

    eventListener.receive(channel, payload);
    logger.info("Published event to " + channel);
  }
}

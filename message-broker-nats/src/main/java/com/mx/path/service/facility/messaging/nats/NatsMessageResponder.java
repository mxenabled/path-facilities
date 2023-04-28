package com.mx.path.service.facility.messaging.nats;

import java.nio.charset.StandardCharsets;

import com.mx.path.core.common.lang.Strings;
import com.mx.path.core.common.messaging.MessageResponder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;

public class NatsMessageResponder implements MessageHandler {
  private Connection connection;
  private Logger logger = LoggerFactory.getLogger(NatsMessageResponder.class);
  private MessageResponder messageResponder;

  public NatsMessageResponder(Connection connection, MessageResponder messageResponder) {
    this.connection = connection;
    this.messageResponder = messageResponder;
  }

  @Override
  public final void onMessage(Message message) throws InterruptedException {
    String payload = new String(message.getData(), StandardCharsets.UTF_8);
    String channel = message.getSubject();

    String response = messageResponder.respond(channel, payload);

    byte[] responseBytes = Strings.isBlank(response) ? new byte[0] : response.getBytes(StandardCharsets.UTF_8);

    connection.publish(message.getReplyTo(), responseBytes);
    logger.info("Published response for " + message.getSubject());
  }
}

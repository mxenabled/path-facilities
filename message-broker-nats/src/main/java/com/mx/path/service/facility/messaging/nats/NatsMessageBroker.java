package com.mx.path.service.facility.messaging.nats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.messaging.EventListener;
import com.mx.path.core.common.messaging.MessageBroker;
import com.mx.path.core.common.messaging.MessageError;
import com.mx.path.core.common.messaging.MessageResponder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;

public class NatsMessageBroker implements MessageBroker {
  private static Logger logger = LoggerFactory.getLogger(NatsMessageBroker.class);

  private NatsConfiguration configuration;
  private Connection connection;
  private List<Dispatcher> dispatchers = new ArrayList<>();

  public NatsMessageBroker(@Configuration NatsConfiguration configurations) {
    this.configuration = configurations;
  }

  @Override
  public final void publish(String channel, String payload) {
    connection().publish(channel, payload.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public final void registerListener(String channel, EventListener listener) {
    MessageHandler messageHandler = new NatsEventListener(listener);
    dispatchers().forEach(dispatcher -> dispatcher.subscribe(channel, channel + ".queue", messageHandler));
    logger.info("Registered listener for " + channel);
  }

  @Override
  public final void registerResponder(String channel, MessageResponder responder) {
    MessageHandler messageResponder = new NatsMessageResponder(connection(), responder);
    dispatchers().forEach(dispatcher -> dispatcher.subscribe(channel, channel + ".queue", messageResponder));
    logger.info("Registered responders for " + channel);
  }

  @Override
  public final String request(String channel, String payload) {
    try {
      Message msg = connection().request(channel, payload.getBytes(StandardCharsets.UTF_8), configuration.getTimeout());

      // io.nats.client.Connection#request docs indicate that a null is returned if timeout is reached
      if (msg == null) {
        throw new NatsMessageBrokerTimeoutException("Request timed out (returned null)");
      }

      return new String(msg.getData(), StandardCharsets.UTF_8);
    } catch (MessageError e) {
      throw e;
    } catch (InterruptedException e) {
      throw new NatsMessageBrokerMessageInterruptedException("Request interrupted", e);
    } catch (Exception e) {
      throw new MessageError("Request failed", e);
    }
  }

  public final void setConfiguration(NatsConfiguration configuration) {
    this.configuration = configuration;
  }

  public final NatsConfiguration getConfigurations() {
    return configuration;
  }

  final void setConnection(Connection connection) {
    this.connection = connection;
  }

  final Connection connection() {
    if (connection == null) {
      if (configuration == null || !configuration.isEnabled()) {
        throw new NatsMessageBrokerDisabledException("Nats messaging is disabled", null);
      }

      try {
        connection = Nats.connect(new NatsConfigurationBuilder(configuration).buildNATSConfiguration());
      } catch (IOException | InterruptedException e) {
        throw new NatsMessageBrokerOperationException("Failed to establish nats connection", e);
      }
    }

    return connection;
  }

  final void addDispatcher(Dispatcher dispatcher) {
    this.dispatchers.add(dispatcher);
  }

  final List<Dispatcher> dispatchers() {
    if (dispatchers.size() == 0) {
      List<Dispatcher> newDispatchers = new ArrayList<>();
      for (int i = 0; i < configuration.getDispatcherCount(); i++) {
        newDispatchers.add(connection().createDispatcher((msg) -> {
        }));
      }
      dispatchers = newDispatchers;
    }

    return dispatchers;
  }
}

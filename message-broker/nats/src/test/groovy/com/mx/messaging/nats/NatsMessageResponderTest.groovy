package com.mx.messaging.nats

import static org.mockito.Mockito.anyString
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import java.nio.charset.StandardCharsets

import com.mx.common.messaging.MessageResponder
import io.nats.client.Connection
import io.nats.client.Message

import spock.lang.Specification

class NatsMessageResponderTest extends Specification {
  Connection connection
  MessageResponder messageResponder
  NatsMessageResponder subject

  def setup() {
    connection = mock(Connection)
    messageResponder = mock(MessageResponder)
    subject = new NatsMessageResponder(connection, messageResponder)
  }

  def "onMessage interacts with messageResponder and connection"() {
    given:
    def requestMessage = "request body"
    def message = mock(Message)

    when(message.getData()).thenReturn(requestMessage.getBytes(StandardCharsets.UTF_8))
    when(message.getSubject()).thenReturn("fake:channel")
    when(message.getReplyTo()).thenReturn("fake:channel:response")

    when(messageResponder.respond(eq("fake:channel"), anyString())).thenReturn("response body")

    when:
    subject.onMessage(message)

    then:
    verify(messageResponder).respond("fake:channel", requestMessage) || true
    verify(connection).publish(eq("fake:channel:response"), eq("response body".getBytes(StandardCharsets.UTF_8))) || true
  }
}

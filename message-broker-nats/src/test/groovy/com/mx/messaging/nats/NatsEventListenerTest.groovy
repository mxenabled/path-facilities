package com.mx.messaging.nats

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import java.nio.charset.StandardCharsets

import com.mx.common.messaging.EventListener
import com.mx.path.service.facility.messaging.nats.NatsEventListener

import io.nats.client.Message

import spock.lang.Specification

class NatsEventListenerTest extends Specification {
  EventListener eventListener
  NatsEventListener subject

  def setup() {
    eventListener = mock(EventListener)
    subject = new NatsEventListener(eventListener)
  }

  def "onMessage interacts with eventListener and connection"() {
    given:
    def messageEvent = "event body"
    def message = mock(Message)

    when(message.getData()).thenReturn(messageEvent.getBytes(StandardCharsets.UTF_8))
    when(message.getSubject()).thenReturn("fake:channel")

    when:
    subject.onMessage(message)

    then:
    verify(eventListener).receive("fake:channel", messageEvent) || true
  }
}

package com.mx.messaging.nats

import static org.mockito.Mockito.any
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import java.nio.charset.StandardCharsets
import java.time.Duration

import com.mx.common.messaging.EventListener
import com.mx.common.messaging.MessageError
import com.mx.common.messaging.MessageResponder
import com.mx.common.messaging.MessageStatus

import org.mockito.Mockito

import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler

import spock.lang.Specification

class NatsMessageBrokerTest extends Specification {
  NatsConfiguration configurations
  Connection connection
  Dispatcher dispatcher
  NatsMessageBroker subject

  def setup() {
    configurations = new NatsConfiguration().tap {
      setServers("nats://127.0.0.1:4222")
      setTimeout(Duration.ofMillis(1000))
    }
    subject = new NatsMessageBroker(configurations)

    dispatcher = mock(Dispatcher)
    connection = mock(Connection)

    subject.addDispatcher(dispatcher)
    subject.setConnection(connection)
  }

  def cleanup() {
    subject.setConfiguration(null)
  }

  def "multi-dispatcher configuration"() {
    given:
    configurations.tap {
      setEnabled(true)
      setDispatcherCount(3)
    }
    subject = spy(new NatsMessageBroker(configurations))

    when:
    Mockito.doReturn(mock(Connection)).when(subject).connection()

    then:
    subject.dispatchers().size() == 3
  }

  def "registerListener interacts with dispatcher"() {
    given:
    def messageDispatcher = mock(EventListener)

    when:
    subject.registerListener("fake.channel", messageDispatcher)

    then:
    verify(dispatcher).subscribe(eq("fake.channel"), eq("fake.channel.queue"), any(MessageHandler)) || true
  }

  def "registerResponder interacts with dispatcher"() {
    given:
    def messageResponder = mock(MessageResponder)

    when:
    subject.registerResponder("fake.channel", messageResponder)

    then:
    verify(dispatcher).subscribe(eq("fake.channel"), eq("fake.channel.queue"), any(MessageHandler)) || true
  }

  def "request interacts with connection"() {
    given:
    def message = mock(Message)
    when(connection.request(eq("fake.channel"), eq("request body".getBytes(StandardCharsets.UTF_8)), any())).thenReturn(message)

    when(message.getData()).thenReturn("response body".getBytes(StandardCharsets.UTF_8))

    when:
    def responseStr = subject.request("fake.channel", "request body")

    then:
    verify(connection).request(eq("fake.channel"), any(), any(Duration)) || true
    responseStr == "response body"
  }

  def "request raises MessageError on nats failure"() {
    given:
    def message = mock(Message)
    when(connection.request(eq("fake.channel"), any(), any())).thenThrow(InterruptedException.class)

    when:
    subject.request("fake.channel", "requestPayload")

    then:
    def ex = thrown MessageError
    ex.message == "Request interrupted"
  }

  def "request raises MessageError on null (timeout) response"() {
    given:
    def message = mock(Message)
    when(connection.request(eq("fake.channel"), any(), any())).thenReturn(null)

    when:
    subject.request("fake.channel", "request body")

    then:
    def ex = thrown MessageError
    ex.message == "Request timed out (returned null)"
    ex.messageStatus == MessageStatus.TIMEOUT
  }

  def "publish interacts with connection"() {
    given:
    def message = mock(Message)
    when(connection.request(eq("fake.channel"), any(), any())).thenReturn(message)

    when:
    subject.publish("fake.channel", "request body")

    then:
    verify(connection).publish(eq("fake.channel"), eq("request body".getBytes(StandardCharsets.UTF_8))) || true
  }

  def "publish raises MessageError on nats failure"() {
    given:
    def message = mock(Message)
    when(connection.request(eq("fake.channel"), any(), any())).thenThrow(InterruptedException.class)

    when:
    subject.request("fake.channel", "request body")

    then:
    thrown MessageError
  }


  def "throws MessageError when no configuration is given"() {
    given:
    subject.setConfiguration(null)
    subject.setConnection(null)

    when:
    subject.request("fake.channel", "request body")

    then:
    thrown(MessageError)
  }

  def "can retrieve configurations"() {
    given:
    subject = new NatsMessageBroker(configurations)

    expect:
    subject.getConfigurations() == configurations
  }
}

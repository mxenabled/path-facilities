package com.mx.messaging.nats

import com.mx.common.collections.ObjectMap
import com.mx.common.messaging.MessageError

import spock.lang.Specification

class NatsConfigurationBuilderTest extends Specification {
  ObjectMap configurations
  NatsConfigurationBuilder subject

  def setup() {
    configurations = new ObjectMap().tap { put("enabled", true) }
    subject = new NatsConfigurationBuilder(new NatsConfiguration(configurations))
  }

  def "builds non ssl configuration"() {
    given:
    configurations.put("servers", "nats://127.0.0.1:4222")

    when:
    def options = subject.buildNATSConfiguration()

    then:
    options.servers.size() == 1
    options.sslContext == null
    options.maxReconnect == -1
  }

  def "builds ssl configuration"() {
    given:
    configurations.tap {
      put("enabled", true)
      put("servers", "nats://127.0.0.1:4222")
      put("tlsCaCertPath", "./src/test/resources/ca.pem")
      put("tlsClientCertPath", "./src/test/resources/cert.pem")
      put("tlsClientKeyPath", "./src/test/resources/key.pem")
    }

    when:
    def options = subject.buildNATSConfiguration()

    then:
    options.getSslContext() != null
  }

  def "can disable tls through configuration"() {
    given:
    configurations.tap {
      put("enabled", true)
      put("servers", "nats://127.0.0.1:4222")
      put("tlsCaCertPath", "./src/test/resources/ca.pem")
      put("tlsClientCertPath", "./src/test/resources/cert.pem")
      put("tlsClientKeyPath", "./src/test/resources/key.pem")
      put("tlsDisabled", true)
    }

    when:
    def options = subject.buildNATSConfiguration()

    then:
    options.getSslContext() == null
  }

  def "fails when disabled"() {
    given:
    configurations.put("enabled", false)

    when:
    def options = subject.buildNATSConfiguration()

    then:
    thrown(NatsMessageBrokerConfigurationException)
  }

  def "fails when not configured"() {
    given:
    subject = new NatsConfigurationBuilder(null)

    when:
    def options = subject.buildNATSConfiguration()

    then:
    thrown(NatsMessageBrokerConfigurationException)
  }
}

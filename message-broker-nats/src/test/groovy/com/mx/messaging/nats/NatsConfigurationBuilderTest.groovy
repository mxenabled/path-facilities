package com.mx.messaging.nats

import com.mx.path.service.facility.messaging.nats.NatsConfiguration
import com.mx.path.service.facility.messaging.nats.NatsConfigurationBuilder
import com.mx.path.service.facility.messaging.nats.NatsMessageBrokerConfigurationException

import spock.lang.Specification

class NatsConfigurationBuilderTest extends Specification {
  NatsConfiguration configurations
  NatsConfigurationBuilder subject

  def setup() {
    configurations = new NatsConfiguration().tap { setEnabled(true) }
    subject = new NatsConfigurationBuilder(configurations)
  }

  def "builds non ssl configuration"() {
    given:
    configurations.setServers("nats://127.0.0.1:4222")

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
      setEnabled(true)
      setServers("nats://127.0.0.1:4222")
      setTlsCaCertPath("./src/test/resources/ca.pem")
      setTlsClientCertPath("./src/test/resources/cert.pem")
      setTlsClientKeyPath("./src/test/resources/key.pem")
    }

    when:
    def options = subject.buildNATSConfiguration()

    then:
    options.getSslContext() != null
  }

  def "can disable tls through configuration"() {
    given:
    configurations.tap {
      setEnabled(true)
      setServers("nats://127.0.0.1:4222")
      setTlsCaCertPath("./src/test/resources/ca.pem")
      setTlsClientCertPath("./src/test/resources/cert.pem")
      setTlsClientKeyPath("./src/test/resources/key.pem")
      setTlsDisabled(true)
    }

    when:
    def options = subject.buildNATSConfiguration()

    then:
    options.getSslContext() == null
  }

  def "fails when disabled"() {
    given:
    configurations.setEnabled(false)

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

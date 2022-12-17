package com.mx.messaging.nats

import java.time.Duration

import com.mx.common.collections.ObjectMap
import com.mx.path.gateway.configuration.ConfigurationBinder
import com.mx.path.gateway.configuration.ConfigurationState

import spock.lang.Specification

class NatsConfigurationTest extends Specification {
  ConfigurationState configurationState = new ConfigurationState()
  ConfigurationBinder binder = new ConfigurationBinder("clientA", configurationState)

  def "test getTimeout"() {
    given:
    def subject = new NatsConfiguration()

    when:
    subject.setTimeoutInMilliseconds(201)

    then: "uses timeoutInMilliseconds"
    subject.getTimeout() == Duration.ofMillis(201)

    when:
    subject.setTimeoutInMilliseconds(null)
    subject.setTimeout(Duration.ofSeconds(2))

    then: "uses timeout"
    subject.getTimeout() == Duration.ofMillis(2000)
  }

  def "binding"() {
    given:
    def configurations = new ObjectMap().tap {
      put("enabled", true)
      put("servers", "nats://127.0.0.1:4223")
      put("dispatcherCount", 9)
    }

    when:
    def result = binder.build(NatsConfiguration, configurations)

    then:
    verifyAll(result) {
      it.isEnabled()
      it.getServers() == "nats://127.0.0.1:4223"
      it.getDispatcherCount() == 9
      it.getTimeout() == Duration.ofSeconds(10) // uses default
    }

    when:
    configurations.tap {
      put("timeoutInMilliseconds", 4000)
    }

    result = binder.build(NatsConfiguration, configurations)

    then:
    verifyAll(result) {
      it.isEnabled()
      it.getServers() == "nats://127.0.0.1:4223"
      it.getDispatcherCount() == 9
      it.getTimeout() == Duration.ofSeconds(4) // uses timeoutInMilliseconds
    }

    when:
    configurations.tap {
      remove("timeoutInMilliseconds")
      put("timeout", "11s")
    }

    result = binder.build(NatsConfiguration, configurations)

    then:
    verifyAll(result) {
      it.isEnabled()
      it.getServers() == "nats://127.0.0.1:4223"
      it.getDispatcherCount() == 9
      it.getTimeout() == Duration.ofSeconds(11) // uses timeout
    }
  }
}

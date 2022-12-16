package com.mx.path.facility.security.vault

import java.time.Duration

import spock.lang.Specification

class VaultEncryptionServiceConfigurationTest extends Specification {
  def "test getRetryInterval"() {
    given:
    def subject = new VaultEncryptionServiceConfiguration()

    when: "using old field"
    subject.setRetryIntervalMilliseconds(201)

    then:
    subject.getRetryInterval() == Duration.ofMillis(201)

    when:
    subject.setRetryInterval(null)
    subject.setRetryIntervalMilliseconds(null)

    then: "uses default"
    subject.getRetryInterval() == Duration.ofMillis(200)

    when:
    subject.setRetryIntervalMilliseconds(null)
    subject.setRetryInterval(Duration.ofSeconds(2))

    then: "uses duration"
    subject.getRetryInterval() == Duration.ofMillis(2000)
  }
}

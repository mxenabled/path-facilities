package com.mx.path.facility.security.vault

import java.time.Duration

import spock.lang.Specification

class VaultEncryptionServiceConfigurationTest extends Specification {
  def "test getRetryInterval"() {
    when:
    def subject = new VaultEncryptionServiceConfiguration()

    then: "uses default"
    subject.getRetryInterval() == Duration.ofMillis(200)

    when:
    subject.setRetryInterval(Duration.ofSeconds(2))

    then: "uses duration"
    subject.getRetryInterval() == Duration.ofMillis(2000)
  }
}

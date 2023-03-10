package com.mx.path.service.facility.store.vault

import java.time.Duration

import spock.lang.Specification

class VaultStoreConfigurationTest extends Specification {
  def "GetRetryInterval"() {
    when:
    def subject = new VaultStoreConfiguration()

    then: "uses default"
    subject.getRetryInterval() == Duration.ofMillis(200)

    when:
    subject.setRetryInterval(Duration.ofSeconds(2))

    then: "uses Duration"
    subject.getRetryInterval() == Duration.ofMillis(2000)
  }
}

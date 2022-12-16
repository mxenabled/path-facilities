package com.mx.path.facility.store.vault

import java.time.Duration

import spock.lang.Specification

class VaultStoreConfigurationTest extends Specification {
  def "GetRetryInterval"() {
    given:
    def subject = new VaultStoreConfiguration()

    when:
    subject.setRetryIntervalMilliseconds(531)

    then: "uses milliseconds"
    subject.getRetryInterval() == Duration.ofMillis(531)

    when:
    subject.setRetryInterval(null)
    subject.setRetryIntervalMilliseconds(null)

    then: "uses default"
    subject.getRetryInterval() == Duration.ofMillis(200)

    when:
    subject.setRetryInterval(Duration.ofSeconds(2))

    then: "uses Duration"
    subject.getRetryInterval() == Duration.ofMillis(2000)
  }
}

package com.mx.path.service.facility.encryption.jasypt;

import com.mx.common.collections.ObjectArray
import com.mx.common.collections.ObjectMap
import com.mx.web.security.JasyptEncryptionService

import spock.lang.Specification

class JasyptEncryptionServiceTest extends Specification {

  ObjectMap configurations
  JasyptEncryptionService subject

  def setup() {
    configurations = new ObjectMap().tap {
      put("enabled", true)
      put("poolSize", 1)
      put("currentKeyIndex", 0)
      put("keys", new ObjectArray().tap {
        add("0123456789")
        add("abcdef")
      })
    }

    subject = new JasyptEncryptionService(configurations)
  }

  def "encryptsWhenEnabled"() {
    given:
    String cyphertext = subject.encrypt("test")

    expect:
    cyphertext.contains("jasypt:781e5e245d69b566979b86e28d23f2c7")
  }

  def "decryptsWhenEnabled"() {
    given:
    def cyphertext = subject.encrypt("test")
    assert "test" != cyphertext

    expect:
    def plaintext = subject.decrypt(cyphertext)
    "test" == plaintext
  }

  def "doesNotEncryptWhenNotEnabled"() {
    given:
    configurations.put("enabled", false)
    subject = new JasyptEncryptionService(configurations)
    def cyphertext = subject.encrypt("test")

    expect:
    "test" == cyphertext
  }

  def "canRetrieveConfigurations"() {
    given:
    subject = new JasyptEncryptionService(configurations)

    expect:
    subject.getConfigurations() == configurations
  }
}

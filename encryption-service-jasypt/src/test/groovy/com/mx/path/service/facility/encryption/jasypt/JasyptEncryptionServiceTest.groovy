package com.mx.path.service.facility.encryption.jasypt;

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import com.mx.path.core.common.collection.ObjectArray
import com.mx.path.core.common.collection.ObjectMap

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.exceptions.EncryptionInitializationException

import spock.lang.Specification
import spock.lang.Unroll

class JasyptEncryptionServiceTest extends Specification implements WithMockery {

  JasyptEncryptionServiceConfiguration configurations
  JasyptEncryptionService subject

  def setup() {
    configurations = new JasyptEncryptionServiceConfiguration().tap {
      setEnabled(true)
      setPoolSize(1)
      setCurrentKeyIndex(0)
      setKeys(new ObjectArray().tap {
        add("0123456789")
        add("abcdef")
      })
    }

    subject = new JasyptEncryptionService(configurations)
  }

  def "encrypt() encrypts when enabled"() {
    when:
    String cyphertext = subject.encrypt("test")

    then:
    cyphertext.contains("jasypt:781e5e245d69b566979b86e28d23f2c7")
  }

  def "encrypt() does noting when not enabled"() {
    when:
    configurations.setEnabled(false)
    subject = new JasyptEncryptionService(configurations)
    def cyphertext = subject.encrypt("test")

    then:
    "test" == cyphertext
  }

  def "encrypt() does nothing when blank"() {
    when:
    configurations.setEnabled(false)
    subject = new JasyptEncryptionService(configurations)
    def cyphertext = subject.encrypt(null)

    then:
    cyphertext == null

    when:
    cyphertext = subject.encrypt("")

    then:
    cyphertext == ""
  }

  def "decrypt() decrypts when enabled"() {
    when:
    def cyphertext = subject.encrypt("test")
    assert "test" != cyphertext

    then:
    def plaintext = subject.decrypt(cyphertext)
    "test" == plaintext
  }

  @Unroll
  def "decrypt() does nothing when not encrypted"() {
    when:
    def plaintext = subject.decrypt(value)

    then:
    plaintext == value

    where:
    value                   | _
    null                    | _
    ""                      | _
    "some unencrypted junk" | _
  }

  def "can retrieve configurations"() {
    when:
    subject = new JasyptEncryptionService(configurations)

    then:
    subject.getConfiguration() == configurations
  }

  def "decrypt() throws configuration exception when no suitable encryptor found"() {
    given:
    subject.getEncryptors().clear()

    when:
    def cyphertext = subject.encrypt("value")
    subject.decrypt(cyphertext)

    then:
    def ex = thrown(JasyptEncryptionOperationException)
    ex.getMessage() == "No suitable decryption key found"
  }

  def "decrypt() wraps jasypt exception when decryption not possible"() {
    when:
    def cyphertext = subject.encrypt("value")
    subject.decrypt(cyphertext + "--junk--")

    then:
    def ex = thrown(JasyptEncryptionOperationException)
    ex.getMessage() == "Decrypt operation failed"
  }

  def "encrypt() wraps jasypt exceptions"() {
    given:
    def currentEncryptor = mock(PooledPBEStringEncryptor)
    subject.setCurrentEncryptor(currentEncryptor)

    when:
    when(currentEncryptor.encrypt("value")).thenThrow(new EncryptionInitializationException("Operation failed"))
    subject.encrypt("value")

    then:
    def ex = thrown(JasyptEncryptionOperationException)
    ex.getMessage() == "Encrypt operation failed"
  }
}

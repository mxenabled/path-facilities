package com.mx.path.service.facility.security.vault

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultException
import com.bettercloud.vault.api.Auth
import com.bettercloud.vault.api.Logical
import com.bettercloud.vault.response.AuthResponse
import com.bettercloud.vault.response.LogicalResponse
import com.bettercloud.vault.rest.RestResponse
import com.mx.path.core.common.collection.ObjectMap

import spock.lang.Specification
import spock.lang.Unroll

class VaultEncryptionServiceTest extends Specification {
  ObjectMap configuration
  Logical logicalDriver
  VaultEncryptionService subject
  Vault vaultDriver

  def setup() {
    logicalDriver = mock(Logical)
    vaultDriver = mock(Vault)
    when(vaultDriver.logical()).thenReturn(logicalDriver)
  }

  def configWithAppId() {
    return new VaultEncryptionServiceConfiguration().tap {
      setUri("http://localhost:8200")
      setEnabled(true)
      setAuthentication(VaultEncryptionServiceConfiguration.AuthenticationType.APPID)
      setAppId("wedge")
      setKeyName("test-key")
      setUserId("1f2cef4b6fe846fd86a4f6730ab74106")
      setMaxRetries(2)
      setNumKeysToKeep(3)
    }
  }

  def configWithToken() {
    return new VaultEncryptionServiceConfiguration().tap {
      setUri("http://localhost:8200")
      setEnabled(true)
      setAuthentication(VaultEncryptionServiceConfiguration.AuthenticationType.TOKEN)
      setToken("token12345")
      setKeyName("test-key")
      setMaxRetries(2)
      setNumKeysToKeep(3)
    }
  }

  def configWithAppRole() {
    return new VaultEncryptionServiceConfiguration().tap {
      setUri("http://localhost:8200")
      setEnabled(true)
      setAuthentication(VaultEncryptionServiceConfiguration.AuthenticationType.APPROLE)
      setAppRole("role-k8s")
      setSecretId("secretId")
      setKeyName("test-key")
      setMaxRetries(2)
      setNumKeysToKeep(3)
    }
  }

  def "on first use, creates and authenticates driver once"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    when(vaultDriver.auth()).thenReturn(authDriver)
    when(vaultDriver.logical()).thenReturn(logicalDriver)

    when(authDriver.loginByAppID("app-id/login", config.getAppId(), config.getUserId())).thenReturn(authResponse)
    when(authResponse.getAuthClientToken()).thenReturn("token12345")

    def encryptResponse = mock(LogicalResponse)
    when(encryptResponse.getData()).thenReturn(Collections.singletonMap("ciphertext", "vault-12341234123412341"))
    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any())).thenReturn(encryptResponse)

    doReturn(vaultDriver).when(subject).buildVaultDriver(any())

    when:
    subject.encrypt("text")
    subject.encrypt("text")

    then:
    // Should only authenticate once
    verify(authDriver, times(1)).loginByAppID("app-id/login", config.getAppId(), config.getUserId()) || true
  }

  @Unroll
  def "buildVaultDriver"() {
    given:
    subject = new VaultEncryptionService(config)

    when:
    def driver = subject.buildVaultDriver(config.getToken())

    then:
    driver.getClass() == Vault

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "decrypt()"() {
    given:
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    def decryptResponse = mock(LogicalResponse)
    when(decryptResponse.getData()).thenReturn(Collections.singletonMap("plaintext", "cGxhaW50ZXh0"))
    when(logicalDriver.write(eq("transit/decrypt/test-key"), any())).thenReturn(decryptResponse)

    when:
    def plaintext = subject.decrypt("vault-12345")
    verify(logicalDriver).write("transit/decrypt/" + config.getKeyName(), Collections.singletonMap("ciphertext", "vault-12345"))

    then:
    plaintext == "plaintext"

    when: "ciphertext is blank"
    plaintext = subject.decrypt("")

    then:
    plaintext == ""

    when: "ciphertext is null"
    plaintext = subject.decrypt(null)

    then:
    plaintext == null

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "decrypt() returns input when not enabled"() {
    given:
    subject = new VaultEncryptionService(new VaultEncryptionServiceConfiguration().tap { setEnabled(false) })

    when:
    def plaintext = subject.decrypt("vault-12345")

    then:
    plaintext == "vault-12345"
  }

  @Unroll
  def "encrypt()"() {
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    def encryptResponse = mock(LogicalResponse)
    when(encryptResponse.getData()).thenReturn(Collections.singletonMap("ciphertext", "vault-12341234123412341"))
    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any())).thenReturn(encryptResponse)

    when:
    def plaintext = subject.encrypt("plaintext")
    verify(logicalDriver).write("transit/encrypt/" + config.getKeyName(), Collections.singletonMap("plaintext", "cGxhaW50ZXh0"))

    then:
    plaintext == "vault-12341234123412341"

    when: "plaintext is blank"
    plaintext = subject.encrypt("")

    then:
    plaintext == ""

    when: "plaintext is null"
    plaintext = subject.encrypt(null)

    then:
    plaintext == null

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "encrypt() returns input when not enabled"() {
    given:
    subject = new VaultEncryptionService(new VaultEncryptionServiceConfiguration().tap { setEnabled(false) })

    when:
    def ciphertext = subject.encrypt("plaintext")

    then:
    ciphertext == "plaintext"
  }

  def "isEncrypted()"() {
    given:
    subject = new VaultEncryptionService(config)

    expect:
    !subject.isEncrypted(null)
    !subject.isEncrypted("")
    !subject.isEncrypted("some crap")
    subject.isEncrypted("vault-1231827361")

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "decrypt rebuilds driver and re-authenticates on errors"() {
    given:
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/decrypt/" + config.getKeyName()), any()))
        .thenReturn(
        new LogicalResponse(new RestResponse(403, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication),
        new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.decrypt("plaintext")

    then:
    noExceptionThrown()
    verify(subject).resetDriver() || true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "decrypt rebuilds driver and re-authenticates on authentication exception"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/decrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))
        .thenReturn(new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.decrypt("ciphertext")

    then:
    noExceptionThrown()
    verify(subject, times(1)).resetDriver() || true
  }

  def "decrypt raises exception if unable to authenticate"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/decrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))

    when:
    subject.decrypt("ciphertext")

    then:
    def ex = thrown(VaultEncryptionAuthenticationException)
    ex.getMessage() == "Permission denied and unable to reauthenticate"
    verify(subject, times(3)).resetDriver() || true
  }

  def "decrypt raises exception operation failed"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/decrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultException("something bad happened"))

    when:
    subject.decrypt("ciphertext")

    then:
    def ex = thrown(VaultEncryptionOperationException)
    ex.getMessage() == "Logical write failed"
    verify(subject, never()).resetDriver() || true
  }

  @Unroll
  def "encrypt() rebuilds driver and re-authenticates on permission denied"() {
    given:
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any()))
        .thenReturn(
        new LogicalResponse(new RestResponse(403, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication),
        new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.encrypt("plaintext")

    then:
    noExceptionThrown()
    verify(subject).resetDriver() || true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "encrypt() rebuilds driver and re-authenticates on authentication exception"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))
        .thenReturn(new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.encrypt("plaintext")

    then:
    noExceptionThrown()
    verify(subject).resetDriver() || true
  }

  def "encrypt() resets driver and raises exception if unable to authenticate after 3 attempts"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))

    when:
    subject.encrypt("plaintext")

    then:
    def ex = thrown(VaultEncryptionAuthenticationException)
    ex.getMessage() == "Permission denied and unable to reauthenticate"
    verify(subject, times(3)).resetDriver() || true
  }

  def "encrypt() raises exception operation failed"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.write(eq("transit/encrypt/" + config.getKeyName()), any()))
        .thenThrow(new VaultException("something bad happened"))

    when:
    subject.encrypt("plaintext")

    then:
    def ex = thrown(VaultEncryptionOperationException)
    ex.getMessage() == "Logical write failed"
    verify(subject, never()).resetDriver() || true
  }

  @Unroll
  def "initKey()"() {
    when:
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    subject.initKey()
    verify(logicalDriver).write("transit/keys/" + config.getKeyName(), Collections.singletonMap("exportable", "true"))

    then:
    true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "initKey does not raise an exception"() {
    given:
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    when(logicalDriver.write("transit/keys/" + config.getKeyName(), Collections.singletonMap("exportable", "true")))
        .thenReturn(new LogicalResponse(new RestResponse(400, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.initKey()

    then:
    noExceptionThrown()

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "rotateKeys()"() {
    given:
    subject = spy(new VaultEncryptionService(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    when(vaultDriver.auth()).thenReturn(authDriver)
    when(vaultDriver.logical()).thenReturn(logicalDriver)

    when(authDriver.loginByAppID("app-id/login", config.getAppId(), config.getUserId())).thenReturn(authResponse)
    when(authDriver.loginByAppRole(config.getAppRole(), config.getSecretId())).thenReturn(authResponse)
    when(authResponse.getAuthClientToken()).thenReturn("token12345")

    def key = VaultTransitKey.builder().keys([
      "1":  123,
      "2":  456,
      "3":  789,
      "4":  1123,
      "5":  1456
    ]).build()

    doReturn(vaultDriver).when(subject).buildVaultDriver(any())
    doReturn(key).when(subject).loadKey()

    when:
    subject.rotateKeys()
    verify(logicalDriver).write("transit/keys/" + config.getKeyName() + "/rotate", null)
    verify(logicalDriver).write("transit/keys/" + config.getKeyName(), Collections.singletonMap("min_decryption_version", 2))

    then:
    true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "rotateKeys() does not raise exceptions"() {
    given:
    def config = configWithAppId().tap {
      setKeyName("key1")
    }
    subject = spy(new VaultEncryptionService(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    when(vaultDriver.auth()).thenReturn(authDriver)
    when(vaultDriver.logical()).thenReturn(logicalDriver)

    when(authDriver.loginByAppID("app-id/login", config.getAppId(), config.getUserId())).thenReturn(authResponse)
    when(authResponse.getAuthClientToken()).thenReturn("token12345")

    doReturn(vaultDriver).when(subject).buildVaultDriver(any())
    doThrow(new RuntimeException("runtime exception")).when(logicalDriver).write("transit/keys/key1/rotate", null)

    when:
    subject.rotateKeys()

    then:
    noExceptionThrown()
  }

  @Unroll
  def "setMinDecryptionVersion() interacts with driver"() {
    when:
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    subject.setMinDecryptionVersion(12)
    verify(logicalDriver).write("transit/keys/" + config.getKeyName(), Collections.singletonMap("min_decryption_version", 12))

    then:
    true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "loadKey()"() {
    given:
    subject = new VaultEncryptionService(config)
    subject.setDriver(vaultDriver)

    def responseBody  = "" +
        "{" +
        "'data' : {" +
        "'type' : 'aes256-gcm96'," +
        "'deletion_allowed' : false," +
        "'derived' : false," +
        "'exportable' : false," +
        "'allow_plaintext_backup' : false," +
        "'keys' : {" +
        "'1' : 1442851412," +
        "'2' : 1442851412," +
        "'3' : 1442851412" +
        "}," +
        "'min_decryption_version' : 1," +
        "'min_encryption_version' : 0," +
        "'name' : 'foo'," +
        "'supports_encryption' : true," +
        "'supports_decryption' : true," +
        "'supports_derivation' : true," +
        "'supports_signing' : false" +
        "}" +
        "}"

    responseBody = responseBody.replace('\'', '\"')

    def restResponse = new RestResponse(200, "application/json", responseBody.getBytes("UTF-8"))
    def logicalResponse = new LogicalResponse(restResponse, 0, Logical.logicalOperations.readV1)
    when(logicalDriver.read("transit/keys/" + config.getKeyName())).thenReturn(logicalResponse);

    when:
    def key = subject.loadKey()

    then:
    key.getClass() == VaultTransitKey

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "loadKeys() rebuilds driver and re-authenticates on permission denied"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.read(eq("transit/keys/" + config.getKeyName())))
        .thenReturn(new LogicalResponse(new RestResponse(403, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))
        .thenReturn(new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.loadKey()

    then:
    noExceptionThrown()
    verify(subject).resetDriver() || true
  }

  def "loadKeys() rebuilds driver and re-authenticates on authentication exception"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.read(eq("transit/keys/" + config.getKeyName())))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))
        .thenReturn(new LogicalResponse(new RestResponse(200, "application/json", "".getBytes("UTF-8")), 0, Logical.logicalOperations.authentication))

    when:
    subject.loadKey()

    then:
    noExceptionThrown()
    verify(subject).resetDriver() || true
  }

  def "loadKeys() resets driver if unable to authenticate, returning null after 3 attempts"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.read(eq("transit/keys/" + config.getKeyName())))
        .thenThrow(new VaultEncryptionAuthenticationException("authentication failed"))

    when:
    def key = subject.loadKey()

    then:
    key == null
    verify(subject, times(3)).resetDriver() || true
  }

  def "loadKeys() returns null when logical read fails"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultEncryptionService(config))
    doReturn(vaultDriver).when(subject).buildAuthenticatedDriver(any())

    when(logicalDriver.read(eq("transit/keys/" + config.getKeyName())))
        .thenThrow(new VaultException("something bad happened"))

    when:
    def key = subject.loadKey()

    then:
    key == null
    verify(subject, never()).resetDriver() || true
  }

  def "can retrieve configurations"() {
    when:
    def config = configWithAppId()
    subject = new VaultEncryptionService(config)

    then:
    subject.getConfiguration() == config
  }
}

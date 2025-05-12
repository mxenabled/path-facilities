package com.mx.path.service.facility.store.vault

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.RETURNS_DEEP_STUBS
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
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

import spock.lang.Specification
import spock.lang.Unroll

class VaultStoreTest extends Specification {
  Logical logicalDriver
  VaultStore subject
  Vault vaultDriver

  def setup() {
    logicalDriver = mock(Logical)
    vaultDriver = mock(Vault)
    doReturn(logicalDriver).when(vaultDriver).logical()
  }

  def configWithAppId() {
    return new VaultStoreConfiguration().tap {
      setUri("http://localhost:8200")
      setAuthentication(VaultStoreConfiguration.AuthenticationType.APPID)
      setAppId("wedge")
      setUserId("1f2cef4b6fe846fd86a4f6730ab74106")
      setMaxRetries(2)
    }
  }

  def configWithToken() {
    return new VaultStoreConfiguration().tap {
      setUri("http://localhost:8200")
      setAuthentication(VaultStoreConfiguration.AuthenticationType.TOKEN)
      setToken("token12345")
      setMaxRetries(2)
    }
  }

  def configWithAppRole() {
    return new VaultStoreConfiguration().tap {
      setUri("http://localhost:8200")
      setAuthentication(VaultStoreConfiguration.AuthenticationType.APPROLE)
      setAppRole("role-k8s")
      setSecretId("secretId")
      setMaxRetries(2)
    }
  }

  def configWithAppRoleSSL() {
    return new VaultStoreConfiguration().tap {
      setUri("http://localhost:8200")
      setAuthentication(VaultStoreConfiguration.AuthenticationType.APPROLE)
      setAppRole("role-k8s")
      setSecretId("secretId")
      setMaxRetries(2)
      setSsl(true)
    }
  }

  def "on first use, creates and authenticates driver once"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultStore(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    doReturn(authDriver).when(vaultDriver).auth()
    doReturn(logicalDriver).when(vaultDriver).logical()

    doReturn(authResponse).when(authDriver).loginByAppID("app-id/login", config.getAppId(), config.getUserId())
    doReturn("token12345").when(authResponse).getAuthClientToken()

    def putResponse = mock(LogicalResponse)
    doReturn(Collections.singletonMap("value", "bar")).when(putResponse).getData()
    doReturn(putResponse).when(logicalDriver).write(eq("secret/foo"), any())

    doReturn(vaultDriver).when(subject).buildVaultDriver(any())

    when:
    subject.put("foo", "bar")
    subject.put("foo", "bar")

    then:
    // Should only authenticate once
    verify(authDriver, times(1)).loginByAppID("app-id/login", config.getAppId(), config.getUserId()) || true
  }

  @Unroll
  def "buildVaultDriver"() {
    given:
    subject = new VaultStore(config)

    when:
    def driver = subject.buildVaultDriver(config.getToken())

    then:
    driver.getClass() == Vault

    where:
    config                  | _
    configWithAppId()       | _
    configWithToken()       | _
    configWithAppRole()     | _
    configWithAppRoleSSL()  | _
  }

  def "buildVaultDriver with invalid configuration"() {
    given:
    def config = new VaultStoreConfiguration().tap {
      setUri(null)
      setMaxRetries(-1)
    }

    subject = new VaultStore(config)

    when:
    subject.buildVaultDriver(config.getToken())

    then:
    def ex = thrown(VaultStoreConfigurationException)
    ex.message == "Unable to build Vault Configuration"
  }

  def "authenticateDriver with missing token"() {
    given:
    def config = new VaultStoreConfiguration().tap {
      setAuthentication(VaultStoreConfiguration.AuthenticationType.TOKEN)
      setToken(null)
    }

    subject = new VaultStore(config)
    subject.buildVaultDriver(config.getToken())

    when:
    subject.get("key")

    then:
    def ex = thrown(VaultStoreConfigurationException)
    ex.message == "Vault token required for TOKEN authentication"
  }

  def "authenticateDriver failed authentication app-role"() {
    given:
    def config = new VaultStoreConfiguration().tap {
      setAuthentication(VaultStoreConfiguration.AuthenticationType.APPROLE)
      setSecretId("secret1")
      setAppRole("app-role1")
    }

    def driver = mock(Vault, RETURNS_DEEP_STUBS)
    subject = spy(new VaultStore(config))
    doReturn(driver).when(subject).buildVaultDriver(null)

    def restResponse = mock(RestResponse)
    when(restResponse.getStatus()).thenReturn(401)

    def response = mock(AuthResponse)
    when(response.getRestResponse()).thenReturn(restResponse)

    when(driver.auth().loginByAppRole("app-role1", "secret1")).thenReturn(response)

    when:
    subject.get("key")

    then:
    def ex = thrown(VaultStoreAuthenticationException)
    ex.message == "Unable to login via app-role (401)"
  }

  @Unroll
  def "put"() {
    given:
    subject = new VaultStore(config)
    subject.setDriver(vaultDriver)

    when:
    def putResponse = mock(LogicalResponse)
    def encodedValue = subject.encodeBase64("bar")
    doReturn(Collections.singletonMap("value", "bar")).when(putResponse).getData()
    doReturn(putResponse).when(logicalDriver).write(eq("secret/foo"), any())
    subject.put("foo", "bar")

    then:
    verify(logicalDriver).write("secret/foo", Collections.singletonMap("value", encodedValue)) || true

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "put with TTL"() {
    given:
    subject = new VaultStore(config)
    subject.setDriver(vaultDriver)

    when:
    subject.put("foo", "bar", 99)

    then:
    thrown(VaultStoreUnsupportedOperation)

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  @Unroll
  def "get"() {
    given:
    subject = new VaultStore(config)
    subject.setDriver(vaultDriver)

    when:
    def getResponse = mock(LogicalResponse)
    doReturn(Collections.singletonMap("value", subject.encodeBase64("bar"))).when(getResponse).getData()
    doReturn(getResponse).when(logicalDriver).read("secret/foo")
    def response = subject.get("foo")
    verify(logicalDriver).read("secret/foo")

    then:
    response == "bar"

    where:
    config              | _
    configWithAppId()   | _
    configWithToken()   | _
    configWithAppRole() | _
  }

  def "get - key not found"() {
    given:
    def config = new VaultStoreConfiguration()
    subject = new VaultStore(config)

    def driver = mock(Vault, RETURNS_DEEP_STUBS)
    subject.setDriver(driver)

    def restResponse = mock(RestResponse)
    when(restResponse.getStatus()).thenReturn(404)

    def response = mock(LogicalResponse)
    when(response.getRestResponse()).thenReturn(restResponse)

    when(driver.logical().read("secret/key")).thenReturn(response)

    when:
    def result = subject.get("key")

    then:
    result == null
  }

  def "delete"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    def deleteResponse = mock(LogicalResponse)
    doReturn(Collections.singletonMap("", "")).when(deleteResponse).getData()
    doReturn(deleteResponse).when(logicalDriver).delete(eq("secret/foo"))
    subject.delete("foo")

    then:
    verify(logicalDriver).delete("secret/foo") || true
  }

  def "deleteSet"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.deleteSet("foo", "bar")

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "getSet"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.getSet("foo")

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "inSet"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.inSet("foo", "bar")

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "putSet with ttl"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.putSet("foo", "bar", 99)

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "putSet"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.putSet("foo", "bar")

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "putIfNotExist"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.putIfNotExist("foo", "bar")

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "putIfNotExist with TTL"() {
    given:
    subject = new VaultStore(configWithAppId())
    subject.setDriver(vaultDriver)

    when:
    subject.putIfNotExist("foo", "bar", 99)

    then:
    thrown(VaultStoreUnsupportedOperation)
  }

  def "Permission Denied Write"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultStore(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    doReturn(authDriver).when(vaultDriver).auth()
    doReturn(logicalDriver).when(vaultDriver).logical()

    doReturn(authResponse).when(authDriver).loginByAppID("app-id/login", config.getAppId(), config.getUserId())
    doReturn("token12345").when(authResponse).getAuthClientToken()

    def putResponse = mock(LogicalResponse)
    doReturn(Collections.singletonMap("value", "bar")).when(putResponse).getData()
    doThrow(new VaultException("permission denied")).when(logicalDriver).write(eq("secret/foo"), any())

    doReturn(vaultDriver).when(subject).buildVaultDriver(any())

    when:
    subject.put("foo", "bar")
    subject.put("foo", "bar")

    then:
    thrown(VaultStoreAuthenticationException)
    // Should only authenticate once
    verify(authDriver, times(4)).loginByAppID("app-id/login", config.getAppId(), config.getUserId()) || true
  }

  @Unroll
  def "Permission Denied Get"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultStore(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    doReturn(authDriver).when(vaultDriver).auth()
    doReturn(logicalDriver).when(vaultDriver).logical()
    doReturn(authResponse).when(authDriver).loginByAppID("app-id/login", config.getAppId(), config.getUserId())
    doThrow(new VaultException("permission denied")).when(logicalDriver).read(eq("secret/foo"))

    when:
    subject.get("foo")

    then:
    thrown(VaultStoreAuthenticationException)
  }

  def "Permission Denied Delete"() {
    given:
    def config = configWithAppId()
    subject = spy(new VaultStore(config))

    def authDriver = mock(Auth)
    def authResponse = mock(AuthResponse)

    doReturn(authDriver).when(vaultDriver).auth()
    doReturn(logicalDriver).when(vaultDriver).logical()
    doReturn(authResponse).when(authDriver).loginByAppID("app-id/login", config.getAppId(), config.getUserId())
    doThrow(new VaultException("permission denied")).when(logicalDriver).delete(eq("secret/foo"))

    when:
    subject.delete("foo")

    then:
    thrown(VaultStoreAuthenticationException)
  }
}

package com.mx.path.service.facility.security.vault

import com.bettercloud.vault.api.Logical
import com.bettercloud.vault.response.LogicalResponse
import com.bettercloud.vault.rest.RestResponse

import spock.lang.Specification

class VaultTransitKeyTest extends Specification {
  def buildJsonObject(String body) {
    body = body.replace('\'', '\"');
    def restResponse = new RestResponse(200, "application/json", body.getBytes())
    def response = new LogicalResponse(restResponse, 0, Logical.logicalOperations.readV1)

    response.getDataObject()
  }

  def "fromJsonObject sets all properties"() {
    given:
    def body  = "{" +
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
        "'min_encryption_version' : 2," +
        "'name' : 'foo'," +
        "'supports_encryption' : true," +
        "'supports_decryption' : true," +
        "'supports_derivation' : true," +
        "'supports_signing' : false" +
        "}" +
        "}"

    when:
    def subject = VaultTransitKey.fromJsonObject(buildJsonObject(body))

    then:
    subject.getName() == "foo"
    subject.getMinDecryptionVersion() == 1
    subject.getMinEncryptionVersion() == 2
    subject.getKeys().size() == 3
    subject.getType() == "aes256-gcm96"
    !subject.isAllowPlaintextBackup()
    !subject.isDeletionAllowed()
    !subject.isDerived()
    !subject.isExportable()
    subject.isSupportsDecryption()
    subject.isSupportsDerivation()
    subject.isSupportsEncryption()
    !subject.isSupportsSigning()
    subject.currentKeyVersion() == 3
  }

  def "fromJsonObject works with blanks"() {
    given:
    String body  = "" +
        "{" +
        "'data' : {" +
        "'keys' : {" +
        "}" +
        "}" +
        "}"

    when:
    def subject = VaultTransitKey.fromJsonObject(buildJsonObject(body))

    then:
    subject.getName() == null
    subject.getMinDecryptionVersion() == 0
    subject.getMinEncryptionVersion() == 0
    subject.getKeys().size() == 0
    subject.getType() == null
    !subject.isAllowPlaintextBackup()
    !subject.isDeletionAllowed()
    !subject.isDerived()
    !subject.isExportable()
    !subject.isSupportsDecryption()
    !subject.isSupportsDerivation()
    !subject.isSupportsEncryption()
    !subject.isSupportsSigning()
    subject.currentKeyVersion() == 0
  }

  def "currentKeyVersion gets the max key version"() {
    given:
    VaultTransitKey key

    when: "keys is null"
    key = VaultTransitKey.builder().keys(null).build()

    then:
    key.currentKeyVersion() == 0

    when: "keys is empty"
    key = VaultTransitKey.builder().keys(new LinkedHashMap<String, Integer>()).build()

    then:
    key.currentKeyVersion() == 0

    when: "keys contains one version"
    key = VaultTransitKey.builder().keys(Collections.singletonMap("4", 1234)).build()

    then:
    key.currentKeyVersion() == 4

    when: "keys contains multiple versions"
    def keys = new LinkedHashMap<String, Integer>()
    keys.put("5", 55)
    keys.put("4", 44)
    keys.put("6", 66)
    keys.put("3", 33)

    key = VaultTransitKey.builder().keys(keys).build()

    then:
    key.currentKeyVersion() == 6
  }
}

package com.mx.path.facility.exception_reporter.honeybadger

import spock.lang.Rollup
import spock.lang.Specification

class HoneybadgerEnvironmentTest extends Specification {

  @Rollup
  def "test value"() {
    when:
    def value = environment.value()

    then:
    value == expectedValue

    where:
    environment                        | expectedValue
    HoneybadgerEnvironment.DEVELOPMENT | "development"
    HoneybadgerEnvironment.SANDBOX     |  "sandbox"
    HoneybadgerEnvironment.QA          |  "qa"
    HoneybadgerEnvironment.INTEGRATION |  "integration"
    HoneybadgerEnvironment.PRODUCTION  |  "production"
  }

  @Rollup
  def "test valueOf"() {
    when:
    def environment = HoneybadgerEnvironment.valueOf(value)

    then:
    environment == expectedEnvironment

    where:
    value | expectedEnvironment
    "DEVELOPMENT" | HoneybadgerEnvironment.DEVELOPMENT
    "SANDBOX"     | HoneybadgerEnvironment.SANDBOX
    "QA"          | HoneybadgerEnvironment.QA
    "INTEGRATION" | HoneybadgerEnvironment.INTEGRATION
    "PRODUCTION"  | HoneybadgerEnvironment.PRODUCTION
  }

  @Rollup
  def "test resolve"() {
    when:
    def environment = HoneybadgerEnvironment.resolve(value)

    then:
    environment == expectedEnvironment

    where:
    value | expectedEnvironment
    "development" | HoneybadgerEnvironment.DEVELOPMENT
    "sandbox"     | HoneybadgerEnvironment.SANDBOX
    "qa"          | HoneybadgerEnvironment.QA
    "integration" | HoneybadgerEnvironment.INTEGRATION
    "production"  | HoneybadgerEnvironment.PRODUCTION
  }
}

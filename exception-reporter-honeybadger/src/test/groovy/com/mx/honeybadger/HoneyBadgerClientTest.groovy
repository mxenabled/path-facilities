package com.mx.honeybadger

import com.mx.path.facility.exception_reporter.honeybadger.HoneyBadgerClient
import com.mx.path.facility.exception_reporter.honeybadger.HoneyBadgerConfiguration

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import org.springframework.core.env.Environment

import io.honeybadger.reporter.HoneybadgerReporter

import spock.lang.Specification

class HoneyBadgerClientTest extends Specification implements WithMockery {

  def setup() {
    def environment = mock(Environment.class)
    String [] environments = ["development"]
    when(environment.getActiveProfiles()).thenReturn(environments)

    new HoneyBadgerConfiguration().tap {
      setEnvironment(environment)
      setApiKey("abcd")
    }
  }

  def cleanup() {
    HoneyBadgerClient.setHoneybadgerReporter(null)
  }

  def "is singleton"() {
    when:
    def reporter = mock(HoneybadgerReporter.class)
    HoneyBadgerClient.setHoneybadgerReporter(reporter)

    then:
    HoneyBadgerClient.getInstance() == reporter
  }

  def "builds new reporter if one does not exist"() {
    given:

    when:
    def reporter = HoneyBadgerClient.getInstance()

    then:
    HoneyBadgerClient.getInstance() == reporter

    when:
    HoneyBadgerClient.setHoneybadgerReporter(null)

    then:
    HoneyBadgerClient.getInstance() != reporter
  }
}

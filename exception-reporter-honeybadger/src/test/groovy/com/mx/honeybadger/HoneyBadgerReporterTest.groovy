package com.mx.honeybadger

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

import com.mx.common.collections.MultiValueMap

import org.mockito.ArgumentCaptor
import org.springframework.core.env.Environment

import io.honeybadger.reporter.HoneybadgerReporter
import io.honeybadger.reporter.config.StandardConfigContext
import io.honeybadger.reporter.dto.Request

import spock.lang.Specification

class HoneyBadgerReporterTest extends Specification implements WithMockery {
  HoneybadgerReporter reporter
  HoneyBadgerReporter subject

  def setup() {
    def environment = mock(Environment.class)
    String [] environments = ["development"]
    when(environment.getActiveProfiles()).thenReturn(environments)

    reporter = mock(HoneybadgerReporter.class)

    new HoneyBadgerConfiguration().tap {
      setEnvironment(environment)
      setApiKey("abcd")
      setAllowedEnvironmentList(Arrays.asList("development"))
      setPackagingPath("com.mx")
    }

    HoneyBadgerClient.setHoneybadgerReporter(reporter)
    when(reporter.getConfig()).thenReturn(new StandardConfigContext())

    subject = new HoneyBadgerReporter()
  }

  def cleanup() {
    HoneyBadgerClient.setHoneybadgerReporter(null)
    new HoneyBadgerConfiguration().setAllowedEnvironmentList(Arrays.asList("qa", "sandbox", "integration", "production"))
  }

  def "with empty context"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext()

    when:
    subject.report(ex, "Holy ~!@#", context)
    verify(reporter).reportError(eq(ex), any(Request.class), eq("Holy ~!@#"))

    then:
    true
  }

  def "with headers context"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext().tap() {
      setHeaders(new MultiValueMap<String, String>().tap() {
        add("accept", "application/json")
        add("user-agent", "some browser")
        add("accept-encoding", "index")
        add("content-length", "123")
        add("accept-language", "en")
        add("accept-charset", "UTF-8")
        add("content-type", "text/xml")
      })
    }

    when:
    subject.report(ex, "Holy ~!@#", context)

    and:
    def requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(reporter).reportError(eq(ex), requestCaptor.capture(), eq("Holy ~!@#"))
    def cgiData = requestCaptor.getValue().getCgiData()

    then:
    cgiData.get("HTTP_ACCEPT") == "application/json"
    cgiData.get("HTTP_USER_AGENT") == "some browser"
    cgiData.get("HTTP_ACCEPT_ENCODING") == "index"
    cgiData.get("CONTENT_LENGTH") == "123"
    cgiData.get("HTTP_ACCEPT_LANGUAGE") == "en"
    cgiData.get("HTTP_ACCEPT_CHARSET") == "UTF-8"
    cgiData.get("CONTENT_TYPE") == "text/xml"
  }

  def "with context data"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext().tap() {
      setMethod("POST")
      setQueryString("q=1")
      setRemoteAddr("bobs-win7")
      setRemotePort(123)
      setServerName("server1")
      setServerPort(456)
      setTraceId("trace-1234")
      setPathInfo("asdf")
      setPathInfoTranslated("fdsa")
      setRequestURL("http://localhost")
    }

    when:
    subject.report(ex, "Holy ~!@#", context)

    and:
    def requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(reporter).reportError(eq(ex), requestCaptor.capture(), eq("Holy ~!@#"))

    def capturedRequest = requestCaptor.getValue()
    def cgiData = capturedRequest.getCgiData()
    def errorContext = capturedRequest.getContext()

    then:
    capturedRequest.getUrl() == "http://localhost?q=1"
    cgiData.get("REQUEST_METHOD") == "POST"
    cgiData.get("QUERY_STRING") == "q=1"
    cgiData.get("REMOTE_ADDR") == "bobs-win7"
    cgiData.get("REMOTE_PORT") == 123
    cgiData.get("SERVER_NAME") == "server1"
    cgiData.get("SERVER_PORT") == 456
    cgiData.get("PATH_INFO") == "asdf"
    cgiData.get("PATH_TRANSLATED") == "fdsa"
    errorContext.get("trace_id") == "trace-1234"
  }

  def "not an allowed environment"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext()
    new HoneyBadgerConfiguration().setAllowedEnvironmentList(Arrays.asList("some_other"))

    when:
    subject.report(ex, "Holy ~!@#", context)
    verify(reporter, never()).reportError(any(), any(), any())

    then:
    true
  }

  def "request_url is built correctly without query string"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext().tap() { setRequestURL("http://localhost") }

    when:
    subject.report(ex, "Holy ~!@#", context)

    and:
    def requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(reporter).reportError(eq(ex), requestCaptor.capture(), eq("Holy ~!@#"))

    def capturedRequest = requestCaptor.getValue()

    then:
    capturedRequest.getUrl() == "http://localhost"
  }

  def "with cookies"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext().tap() {
      setHeaders(new MultiValueMap<String, String>().tap() {
        put("Set-Cookie", Arrays.asList("q=1", "f=3"))
      })
    }

    when:
    subject.report(ex, "Holy ~!@#", context)

    and:
    def requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(reporter).reportError(eq(ex), requestCaptor.capture(), eq("Holy ~!@#"))

    def capturedRequest = requestCaptor.getValue()
    def cgiData = capturedRequest.getCgiData()

    then:
    cgiData.get("HTTP_COOKIE") == "q=1; f=3"
  }

  def "with parameters"() {
    given:
    def ex = new RuntimeException("something is broke")
    def context = new TestExceptionContext().tap() {
      setParameters(new MultiValueMap<String, String>().tap() {
        put("multi", Arrays.asList("a", "b"))
        add("single", "c")
      })
    }

    when:
    subject.report(ex, "Holy ~!@#", context)

    and:
    def requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(reporter).reportError(eq(ex), requestCaptor.capture(), eq("Holy ~!@#"))

    def capturedRequest = requestCaptor.getValue()
    def params = capturedRequest.getParams()

    then:
    params.get("multi") == "[ a, b ]"
    params.get("single") == "c"
  }
}

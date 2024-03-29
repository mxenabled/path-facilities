package com.mx.path.service.facility.exception_reporter.honeybadger

import com.mx.path.core.common.collection.MultiValueMap
import com.mx.path.core.common.exception.ExceptionContext


class TestExceptionContext implements ExceptionContext {
  Map<String, String> context
  String environment = "development"
  MultiValueMap<String, String> headers
  MultiValueMap<String, String> parameters
  String method
  String pathInfo
  String pathInfoTranslated
  String queryString
  String remoteAddr
  int remotePort
  String requestURL
  String serverName
  int serverPort
  String serverProtocol
  long sessionCreateTime
  String sessionId
  String traceId
  String userId

  @Override
  Map<String, String> getContext() {
    return context
  }

  @Override
  String getEnvironment() {
    return environment
  }

  @Override
  MultiValueMap<String, String> getHeaders() {
    return headers
  }

  @Override
  MultiValueMap<String, String> getParameters() {
    return parameters
  }

  @Override
  String getMethod() {
    return method
  }

  @Override
  String getPathInfo() {
    return pathInfo
  }

  @Override
  String getPathTranslated() {
    return pathInfoTranslated
  }

  @Override
  String getQueryString() {
    return queryString
  }

  @Override
  String getRemoteAddr() {
    return remoteAddr
  }

  @Override
  int getRemotePort() {
    return remotePort
  }

  @Override
  String getRequestURL() {
    return requestURL
  }

  @Override
  String getServerName() {
    return serverName
  }

  @Override
  int getServerPort() {
    return serverPort
  }

  @Override
  String getServerProtocol() {
    return serverProtocol
  }

  @Override
  long getSessionCreateTime() {
    return sessionCreateTime
  }

  @Override
  String getSessionId() {
    return sessionId
  }

  @Override
  String getTraceId() {
    return traceId
  }

  @Override
  String getUserId() {
    return userId
  }
}

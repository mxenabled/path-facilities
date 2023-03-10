package com.mx.path.service.facility.exception_reporter.honeybadger.spring;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mx.common.collections.MultiValueMap;
import com.mx.common.exception.ExceptionContext;
import com.mx.common.exception.ExceptionReporter;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.config.ConfigContext;
import io.honeybadger.reporter.dto.CgiData;
import io.honeybadger.reporter.dto.Context;
import io.honeybadger.reporter.dto.Params;
import io.honeybadger.reporter.dto.Request;
import io.honeybadger.reporter.dto.Session;

@Component
@Deprecated
public class HoneyBadgerReporter implements ExceptionReporter {

  // Public

  @Override
  public final void report(@NonNull Throwable ex, @Nullable String message, @NonNull ExceptionContext context) {
    String[] profileNames = HoneyBadgerConfiguration.getEnvironment().getActiveProfiles();
    if (!HoneyBadgerConfiguration.getAllowedEnvironmentList().contains(profileNames[0])) {
      return;
    }

    HoneybadgerReporter reporter = HoneyBadgerClient.getInstance();
    CgiData cgiData = createCgiData(context);
    Context cxt = createContext(context);
    Params params = createParams(context, reporter.getConfig());
    Session session = createSession(context);
    Request request = new Request(cxt, getFullURL(context), params, session, cgiData);

    reporter.reportError(ex, request, message);
  }

  // Private

  private CgiData createCgiData(final ExceptionContext context) {
    CgiData result = new CgiData()
        .setHttpCookie(parseCookies(context))
        .setPathInfo(context.getPathInfo())
        .setPathTranslated(context.getPathTranslated())
        .setQueryString(context.getQueryString())
        .setRemoteAddr(context.getRemoteAddr())
        .setRemotePort(context.getRemotePort())
        .setRequestMethod(context.getMethod())
        .setServerName(context.getServerName())
        .setServerPort(context.getServerPort())
        .setServerProtocol(context.getServerProtocol());

    MultiValueMap<String, String> headers = context.getHeaders();
    if (headers != null) {
      result.addFromHttpHeaders(headers.toSingleValueMap());
    }

    return result;
  }

  private Context createContext(@NonNull ExceptionContext context) {
    Context cxt = new Context();
    cxt.put("client_id", context.getClientId());
    cxt.put("feature", context.getFeature());
    cxt.put("session_trace_id", context.getSessionTraceId());
    cxt.put("trace_id", context.getTraceId());
    cxt.put("user_id", context.getUserId());

    return cxt;
  }

  private Params createParams(ExceptionContext context, ConfigContext configContext) {
    final Params params = new Params(configContext);

    params.put("_method", context.getMethod());
    MultiValueMap<String, String> parameters = context.getParameters();
    if (parameters != null) {
      parameters.forEach((k, v) -> {
        if (v.size() > 1) {
          String valString = "[ "
              + v.stream().filter(Objects::nonNull).collect(Collectors.joining(", "))
              + " ]";
          params.put(k, valString);
        } else {
          params.put(k, v.get(0));
        }
      });
    }

    return params;
  }

  private Session createSession(ExceptionContext context) {
    final Session session = new Session();
    session.put("session_id", context.getSessionId());
    session.put("creation_time", context.getSessionCreateTime());

    return session;
  }

  /**
   * Gets the fully formed URL for a servlet context.
   *
   * @param context Servlet context to parse for URL information
   * @return fully formed URL as string
   * @see <a href="http://stackoverflow.com/a/2222268/33611">Stack Overflow Answer</a>
   */
  private String getFullURL(final ExceptionContext context) {
    String requestURL = context.getRequestURL();
    String queryString = context.getQueryString();

    if (requestURL == null) {
      return null;
    } else if (queryString == null) {
      return requestURL;
    } else {
      return requestURL + '?' + queryString;
    }
  }

  private String parseCookies(final ExceptionContext context) {
    MultiValueMap<String, String> headers = context.getHeaders();
    if (headers == null) {
      return null;
    }

    List<String> cookieHeaders = headers.get("Set-Cookie");
    if (cookieHeaders == null) {
      return null;
    }

    return cookieHeaders.stream().filter(Objects::nonNull).collect(Collectors.joining("; "));
  }
}

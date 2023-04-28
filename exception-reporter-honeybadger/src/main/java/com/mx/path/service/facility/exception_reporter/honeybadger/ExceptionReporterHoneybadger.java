package com.mx.path.service.facility.exception_reporter.honeybadger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mx.path.core.common.collection.MultiValueMap;
import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.exception.ExceptionContext;
import com.mx.path.core.common.exception.ExceptionReporter;
import com.mx.path.core.common.lang.Strings;

import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.config.ConfigContext;
import io.honeybadger.reporter.config.StandardConfigContext;
import io.honeybadger.reporter.dto.CgiData;
import io.honeybadger.reporter.dto.Context;
import io.honeybadger.reporter.dto.Params;
import io.honeybadger.reporter.dto.Request;
import io.honeybadger.reporter.dto.Session;

public class ExceptionReporterHoneybadger implements ExceptionReporter {
  /**
   * HoneyBadger reporters, by environment
   */
  private final Map<String, HoneybadgerReporter> honeybadgerReporters = new HashMap<>();
  private final HoneyBadgerConfiguration configuration;

  public ExceptionReporterHoneybadger(@Configuration HoneyBadgerConfiguration configuration) {
    this.configuration = configuration;
  }

  final HoneybadgerReporter getReporter(String environment) {
    if (!honeybadgerReporters.containsKey(environment)) {
      synchronized (ExceptionReporterHoneybadger.class) {
        StandardConfigContext config = new StandardConfigContext();
        config.setApiKey(configuration.getApiKey())
            .setEnvironment(environment)
            .setApplicationPackage(configuration.getPackagingPath());

        honeybadgerReporters.put(environment, new HoneybadgerReporter(config));
      }
    }

    return honeybadgerReporters.get(environment);
  }

  @Override
  public final void report(Throwable ex, String message, ExceptionContext context) {
    if (Strings.isBlank(context.getEnvironment()) || !configuration.getAllowedEnvironments().contains(context.getEnvironment())) {
      return;
    }

    HoneybadgerReporter honeybadgerReporter = getReporter(context.getEnvironment());

    CgiData cgiData = createCgiData(context);
    Context cxt = createContext(context);
    Params params = createParams(context, honeybadgerReporter.getConfig());
    Session session = createSession(context);
    Request request = new Request(cxt, getFullURL(context), params, session, cgiData);

    honeybadgerReporter.reportError(ex, request, message);
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

  private Context createContext(ExceptionContext context) {
    Context cxt = new Context();
    addIdPresent(cxt, "client_id", context.getClientId());
    addIdPresent(cxt, "feature", context.getFeature());
    addIdPresent(cxt, "session_trace_id", context.getSessionTraceId());
    addIdPresent(cxt, "trace_id", context.getTraceId());
    addIdPresent(cxt, "user_id", context.getUserId());

    return cxt;
  }

  private void addIdPresent(Context context, String key, String value) {
    if (Strings.isNotBlank(value)) {
      context.put(key, value);
    }
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

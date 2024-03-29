package com.mx.path.service.facility.fault_tolerant_executor.resilience4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.mx.path.core.common.process.FaultTolerantScopeConfiguration;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.BulkheadConfigurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.CircuitBreakerConfigurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.ConfigurationException;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.ConfigurationUtils;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.Configurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.Resilience4jConfigurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.ScopedResilience4jConfigurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.ThreadPoolBulkheadConfigurations;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.TimeLimiterConfigurations;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

/**
 * Manages Resilience4j configuration scopes. The registry should be initialized via the {@link #initializeFromConfigurations(Configurations)} initializeFromConfigurations
 * method. After initialization, you can retrieve a configured {@link CircuitBreaker}, {@link Bulkhead}, and/or {@link TimeLimiter} for
 * a provided scope. If the provided scope doesn't match any registered scopes, the default configurations will be used.
 */
public final class ScopedConfigurationRegistry {
  private static final String DEFAULT_SCOPE = "DEFAULT";
  private static final Pattern SCOPE_PATTERN = Pattern.compile("^[0-9a-zA-Z_]+(\\.[0-9a-zA-Z_]+)*$");

  @Getter(AccessLevel.PACKAGE)
  private final Map<String, FaultTolerantScopeConfiguration> faultTolerantScopeConfigurationMap = new ConcurrentHashMap<>();

  @Setter(AccessLevel.PACKAGE)
  @Getter(AccessLevel.PACKAGE)
  private List<ScopedResilience4jConfigurations> configurationRegistry = new ArrayList<>();

  @Setter(AccessLevel.PACKAGE)
  @Getter(AccessLevel.PACKAGE)
  private ScopedResilience4jConfigurations defaultConfigurations;

  @Setter(AccessLevel.PACKAGE)
  private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

  @Setter(AccessLevel.PACKAGE)
  private BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();

  @Setter(AccessLevel.PACKAGE)
  private TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

  @Setter(AccessLevel.PACKAGE)
  private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();

  public void initializeFromConfigurations(Configurations configurations) {
    Resilience4jConfigurations defaults = mergeConfigurations(Resilience4jConfigurations.builder().build().withDefaults(), configurations.getDefaults());
    registerConfigurations(DEFAULT_SCOPE, defaults);

    // Cache the default configurations for speedy access.
    defaultConfigurations = new ScopedResilience4jConfigurations();
    defaultConfigurations.setConfigurations(defaults);
    defaultConfigurations.setScope(DEFAULT_SCOPE);

    for (ScopedResilience4jConfigurations scopedConfiguration : configurations.getScopes()) {
      if (!isValidScopeSyntax(scopedConfiguration.getScope())) {
        throw new RuntimeException("Malformed scope syntax: " + scopedConfiguration.getScope());
      }
      registerConfigurations(scopedConfiguration.getScope(), mergeConfigurations(defaults, scopedConfiguration.getConfigurations()));
    }
    // Sorts the registry from longest to smallest character-length scope. This allows us to look up configuration without
    // doing string manipulation.
    configurationRegistry.sort((a, b) -> Integer.compare(b.getScope().length(), a.getScope().length()));
  }

  /**
   * Returns a CircuitBreaker for the provided scope, or a default CircuitBreaker if there is not a scope match.
   *
   * <p>Returns null if the CircuitBreaker facility has been disabled.
   *
   * @param scope scope
   * @return CircuitBreaker for scope
   */
  public CircuitBreaker getCircuitBreaker(String scope) {
    ScopedResilience4jConfigurations scopedConfigs = findBestConfigurations(scope);
    if (!Objects.equals(scopedConfigs.getConfigurations().getCircuitBreakerConfigurations().getEnabled(), Boolean.TRUE)) {
      return null;
    }

    return circuitBreakerRegistry.circuitBreaker(scopedConfigs.getScope());
  }

  /**
   * Returns a Bulkhead for the provided scope, or a default Bulkhead if there is not a scope match.
   *
   * <p>Returns null if the Bulkhead facility has been disabled.
   *
   * @param scope scope
   * @return Bulkhead for scope
   */
  public Bulkhead getBulkhead(String scope) {
    ScopedResilience4jConfigurations scopedConfigs = findBestConfigurations(scope);
    if (!Objects.equals(scopedConfigs.getConfigurations().getBulkheadConfigurations().getEnabled(), Boolean.TRUE)) {
      return null;
    }

    return bulkheadRegistry.bulkhead(scopedConfigs.getScope());
  }

  /**
   * Returns a TimeLimiter for the provided scope, or a default TimeLimiter if there is not a scope match.
   *
   * <p>Returns null if the TimeLimiter facility has been disabled.
   *
   * @param scope scope
   * @return TimeLimiter for scope
   */
  public TimeLimiter getTimeLimiter(String scope) {
    ScopedResilience4jConfigurations scopedConfigs = findBestConfigurations(scope);
    if (!Objects.equals(scopedConfigs.getConfigurations().getTimeLimiterConfigurations().getEnabled(), Boolean.TRUE)) {
      return null;
    }

    return timeLimiterRegistry.timeLimiter(scopedConfigs.getScope());
  }

  /**
   * Returns a ThreadPoolBulkhead for the provided scope, or a default ThreadPoolBulkhead if there is not a scope match.
   *
   * <p>Returns null if the ThreadPoolBulkhead facility has been disabled.
   *
   * @param scope scope
   * @return ThreadPoolBulkhead for scope
   */
  public ThreadPoolBulkhead getThreadPoolBulkhead(String scope) {
    ScopedResilience4jConfigurations scopedConfigs = findBestConfigurations(scope);
    if (!Objects.equals(scopedConfigs.getConfigurations().getThreadPoolBulkheadConfigurations().getEnabled(), Boolean.TRUE)) {
      return null;
    }

    return threadPoolBulkheadRegistry.bulkhead(scopedConfigs.getScope());
  }

  /**
   * Retrieves the {@link FaultTolerantScopeConfiguration} for the provided scope. Caches the instance in the
   * `faultTolerantScopeConfigurationMap` since it is _highly_ likely more tasks will be submitted for the provided
   * scope.
   *
   * @param scope scope
   * @return FaultTolerantScopeConfiguration for scope
   */
  public FaultTolerantScopeConfiguration getFaultTolerantScopeConfiguration(String scope) {
    if (!faultTolerantScopeConfigurationMap.containsKey(scope)) {
      faultTolerantScopeConfigurationMap.put(scope, findBestConfigurations(scope).toFaultTolerantScopeConfiguration());
    }

    return faultTolerantScopeConfigurationMap.get(scope);
  }

  /**
   * Searches the registered configurations for a scope match. If no direct match is found, another search is done
   * for a partial match. If no match is found in either case, the default configurations are returned.
   *
   * @param scope scope
   * @return ScopedResilience4jConfigurations for scope
   */
  @NonNull
  ScopedResilience4jConfigurations findBestConfigurations(String scope) {
    // First pass looks for a direct match.
    for (ScopedResilience4jConfigurations config : configurationRegistry) {
      if (scope.equalsIgnoreCase(config.getScope())) {
        return config;
      }
    }

    // Second pass looks for a partial match.
    for (ScopedResilience4jConfigurations config : configurationRegistry) {
      if (scope.contains(config.getScope())) {
        return config;
      }
    }

    return defaultConfigurations;
  }

  /**
   * Creates a copy of the default configurations, applies the overrides, and returns the new configurations. Does not
   * mutate the input.
   *
   * @param defaults configurations defaults
   * @param overrides configuration overrides
   */
  Resilience4jConfigurations mergeConfigurations(Resilience4jConfigurations defaults, Resilience4jConfigurations overrides) {
    ConfigurationUtils configUtils = new ConfigurationUtils();
    Resilience4jConfigurations copy = defaults.deepCopy();

    configUtils.mergeNonNullProperties(copy.getBulkheadConfigurations(), overrides.getBulkheadConfigurations());
    configUtils.mergeNonNullProperties(copy.getCircuitBreakerConfigurations(), overrides.getCircuitBreakerConfigurations());
    configUtils.mergeNonNullProperties(copy.getTimeLimiterConfigurations(), overrides.getTimeLimiterConfigurations());
    configUtils.mergeNonNullProperties(copy.getThreadPoolBulkheadConfigurations(), overrides.getThreadPoolBulkheadConfigurations());

    return copy;
  }

  /**
   * Uses the supplied configurations to register a bulkhead,
   * circuit breaker, and time limiter for the provided scope.
   *
   * @param scope scope
   * @param configurations for scope
   */
  void registerConfigurations(String scope, Resilience4jConfigurations configurations) {
    ScopedResilience4jConfigurations scopedConfigurations = new ScopedResilience4jConfigurations();
    scopedConfigurations.setScope(scope);
    scopedConfigurations.setConfigurations(configurations);
    configurationRegistry.add(scopedConfigurations);

    bulkheadRegistry.bulkhead(scope, buildBulkheadConfig(configurations.getBulkheadConfigurations()));
    circuitBreakerRegistry.circuitBreaker(scope, buildCircuitBreakerConfig(configurations.getCircuitBreakerConfigurations()));
    timeLimiterRegistry.timeLimiter(scope, buildTimeLimiterConfig(configurations.getTimeLimiterConfigurations()));
    threadPoolBulkheadRegistry.bulkhead(scope, buildThreadPoolBulkheadConfig(configurations.getThreadPoolBulkheadConfigurations()));
  }

  /**
   * A scope must be a dot (.) delimited string.
   *
   * @param scope scope
   * @return true, if given scope is valid
   */
  boolean isValidScopeSyntax(String scope) {
    return SCOPE_PATTERN.matcher(scope).matches();
  }

  private CircuitBreakerConfig buildCircuitBreakerConfig(CircuitBreakerConfigurations config) {
    CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
        .failureRateThreshold(config.getFailureRateThreshold())
        .slowCallRateThreshold(config.getSlowCallRateThreshold())
        .slowCallDurationThreshold(config.getSlowCallDurationThreshold())
        .permittedNumberOfCallsInHalfOpenState(config.getPermittedNumberOfCallsInHalfOpenState())
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(config.getSlidingWindowType()))
        .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
        .waitDurationInOpenState(config.getWaitDurationInOpenState())
        .automaticTransitionFromOpenToHalfOpenEnabled(config.getAutomaticTransitionFromOpenToHalfOpenEnabled());

    // The internal default for this value is 0; however, if we try to provide that to the `CircuitBreakerConfig.Builder`
    // an exception will be thrown.
    if (config.getMaxWaitDurationInHalfOpenState().toMillis() > 0) {
      builder.maxWaitDurationInHalfOpenState(config.getMaxWaitDurationInHalfOpenState());
    }

    if (config.getSlidingWindowType().equalsIgnoreCase("COUNT_BASED")) {
      if (config.getSlidingWindowTaskCount() == null) {
        throw new ConfigurationException("slidingWindowTaskCount must be provided when slidingWindowType is COUNT_BASED");
      }

      builder.slidingWindowSize(config.getSlidingWindowTaskCount());
    } else if (config.getSlidingWindowType().equalsIgnoreCase("TIME_BASED")) {
      if (config.getSlidingWindowDuration() == null) {
        throw new ConfigurationException("slidingWindowDurationSeconds must be provided when slidingWindowType is TIME_BASED");
      }

      builder.slidingWindowSize((int) config.getSlidingWindowDuration().getSeconds());
    } else {
      throw new ConfigurationException("slidingWindowType must be COUNT_BASED or TIME_BASED");
    }

    return builder.build();
  }

  private BulkheadConfig buildBulkheadConfig(BulkheadConfigurations config) {
    return BulkheadConfig.custom()
        .maxConcurrentCalls(config.getMaxConcurrentCalls())
        .maxWaitDuration(config.getMaxWaitDuration())
        .build();
  }

  private TimeLimiterConfig buildTimeLimiterConfig(TimeLimiterConfigurations config) {
    return TimeLimiterConfig.custom()
        .timeoutDuration(config.getTimeoutDuration())
        .build();
  }

  private ThreadPoolBulkheadConfig buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfigurations config) {
    return ThreadPoolBulkheadConfig.custom()
        .maxThreadPoolSize(config.getMaxThreadPoolSize())
        .coreThreadPoolSize(config.getCoreThreadPoolSize())
        .queueCapacity(config.getQueueCapacity())
        .keepAliveDuration(config.getKeepAlive())
        .build();
  }
}

package com.mx.path.service.facility.fault_tolerant_executor.resilience4j;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import lombok.AccessLevel;
import lombok.Setter;

import com.mx.path.core.common.accessor.PathResponseStatus;
import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.connect.ConnectException;
import com.mx.path.core.common.connect.ServiceUnavailableException;
import com.mx.path.core.common.connect.TooManyRequestsException;
import com.mx.path.core.common.exception.PathRequestException;
import com.mx.path.core.common.exception.PathSystemException;
import com.mx.path.core.common.process.FaultTolerantExecutor;
import com.mx.path.core.common.process.FaultTolerantTask;
import com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration.Configurations;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;

public final class Resilience4jFaultTolerantExecutor implements FaultTolerantExecutor {
  @Setter(AccessLevel.PACKAGE)
  private ScopedConfigurationRegistry scopedConfigurationRegistry;

  public Resilience4jFaultTolerantExecutor(@Configuration Configurations configurations) {
    this.scopedConfigurationRegistry = new ScopedConfigurationRegistry();
    this.scopedConfigurationRegistry.initializeFromConfigurations(configurations);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  @Override
  public void submit(String scope, FaultTolerantTask task) {
    try {
      executeDecoratorStack(scope, task);
    } catch (BulkheadFullException e) {
      throw new TooManyRequestsException(
          "Resilience4j bulkhead is full. Rejecting task.",
          e);
    } catch (CallNotPermittedException e) {
      throw new ServiceUnavailableException(
          "Resilience4j circuit breaker is open. Rejecting task.",
          e);
    } catch (TimeoutException e) {
      throw new com.mx.path.core.common.connect.TimeoutException(
          "Resilience4j triggered a timeout.",
          e);
    } catch (PathRequestException | PathSystemException e) {
      throw e; // rethrow Path exceptions
    } catch (Exception e) {
      throw new ConnectException(
          "An unknown error occurred",
          PathResponseStatus.INTERNAL_ERROR,
          e);
    }
  }

  /**
   * Executes the decorator stack for the provided scope and task.
   *
   * @param scope
   * @param task
   * @throws Exception
   */
  void executeDecoratorStack(String scope, FaultTolerantTask task) throws Exception {
    withBulkhead(scope,
        withThreadPoolBulkhead(scope,
            withCircuitBreaker(scope,
                withTimeLimiter(scope, task))))
        .call();
  }

  private Callable<?> withBulkhead(String scope, Callable<?> callable) {
    Bulkhead bulkhead = scopedConfigurationRegistry.getBulkhead(scope);
    if (bulkhead == null) {
      return callable;
    }

    return Bulkhead.decorateCallable(bulkhead, callable);
  }

  private Callable<?> withCircuitBreaker(String scope, Callable<?> callable) {
    CircuitBreaker circuitBreaker = scopedConfigurationRegistry.getCircuitBreaker(scope);
    if (circuitBreaker == null) {
      return callable;
    }

    return CircuitBreaker.decorateCallable(circuitBreaker, callable);
  }

  private Callable<?> withTimeLimiter(String scope, FaultTolerantTask task) {
    TimeLimiter timeLimiter = scopedConfigurationRegistry.getTimeLimiter(scope);
    if (timeLimiter == null) {
      return () -> {
        task.apply(scopedConfigurationRegistry.getFaultTolerantScopeConfiguration(scope));
        return null;
      };
    }

    return timeLimiter.decorateFutureSupplier(() -> CompletableFuture.supplyAsync(() -> {
      task.apply(scopedConfigurationRegistry.getFaultTolerantScopeConfiguration(scope));
      return null;
    }));
  }

  private Callable<?> withThreadPoolBulkhead(String scope, Callable<?> callable) {
    ThreadPoolBulkhead threadPoolBulkhead = scopedConfigurationRegistry.getThreadPoolBulkhead(scope);
    if (threadPoolBulkhead == null) {
      return callable;
    }

    return () -> ThreadPoolBulkhead.decorateCallable(threadPoolBulkhead, callable).get().toCompletableFuture().get();
  }
}

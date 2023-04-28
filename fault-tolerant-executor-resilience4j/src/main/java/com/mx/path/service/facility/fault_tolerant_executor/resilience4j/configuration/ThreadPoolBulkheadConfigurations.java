package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.mx.path.core.common.configuration.ConfigurationField;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public final class ThreadPoolBulkheadConfigurations implements PopulatesDefaults<ThreadPoolBulkheadConfigurations> {
  private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_CORE_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_QUEUE_CAPACITY = 1;
  private static final int DEFAULT_KEEP_ALIVE_MILLIS = 20;

  /**
   * Determines whether a thread-pool bulkhead is enabled or not.
   */
  @ConfigurationField
  private Boolean enabled;

  /**
   * Configures the max thread pool size.
   */
  @ConfigurationField
  private Integer maxThreadPoolSize;

  /**
   * Configures the core thread pool size
   */
  @ConfigurationField
  private Integer coreThreadPoolSize;

  /**
   * Configures the capacity of the queue.
   */
  @ConfigurationField
  private Integer queueCapacity;

  /**
   * When the number of threads is greater than the core, this is the maximum time that excess idle threads will wait
   * for new tasks before terminating.
   */
  @ConfigurationField
  private Duration keepAlive;

  @Override
  public ThreadPoolBulkheadConfigurations withDefaults() {
    maxThreadPoolSize = DEFAULT_MAX_THREAD_POOL_SIZE;
    coreThreadPoolSize = DEFAULT_CORE_THREAD_POOL_SIZE;
    queueCapacity = DEFAULT_QUEUE_CAPACITY;
    keepAlive = Duration.ofMillis(DEFAULT_KEEP_ALIVE_MILLIS);

    return this;
  }
}

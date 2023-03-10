package com.mx.path.service.facility.fault_tolerant_executor.resilience4j.configuration;

public interface PopulatesDefaults<T extends PopulatesDefaults<T>> {
  T withDefaults();
}

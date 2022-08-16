package com.mx.path.service.facility.fault_tolerant_executor.configuration;

public interface PopulatesDefaults<T extends PopulatesDefaults<T>> {
  T withDefaults();
}

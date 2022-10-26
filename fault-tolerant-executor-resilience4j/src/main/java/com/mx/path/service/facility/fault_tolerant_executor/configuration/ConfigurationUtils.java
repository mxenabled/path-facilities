package com.mx.path.service.facility.fault_tolerant_executor.configuration;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.NonNull;

public final class ConfigurationUtils {
  /**
   * Utility method for easily merging configuration objects. Ignores source-properties that are null.
   *
   * @param destination object
   * @param source object
   * @param <T> type of configuration objects
   */
  public <T> void mergeNonNullProperties(@NonNull T destination, @NonNull T source) {
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(source.getClass());
      for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
        Method getter = propertyDescriptor.getReadMethod();
        Method setter = propertyDescriptor.getWriteMethod();

        if (getter == null || setter == null) {
          continue;
        }

        Object value = getter.invoke(source);
        if (value != null) {
          setter.invoke(destination, value);
        }
      }

    } catch (IntrospectionException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("An error occurred merging configurations", e);
    }

  }
}

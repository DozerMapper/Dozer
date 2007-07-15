/*
 * Copyright 2005-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.dozer.util.mapping.propertydescriptor;

import java.lang.reflect.Field;

import net.sf.dozer.util.mapping.fieldmap.FieldMap;
import net.sf.dozer.util.mapping.fieldmap.HintContainer;
import net.sf.dozer.util.mapping.util.MappingUtils;
import net.sf.dozer.util.mapping.util.ReflectionUtils;

/**
 * Internal class that directly accesses the field via reflection. The getter/setter methods for the field are bypassed
 * and will NOT be invoked. Private fields are accessible by Dozer. Only intended for internal use.
 * 
 * @author garsombke.franz
 * 
 */
public class FieldPropertyDescriptor extends AbstractPropertyDescriptor implements DozerPropertyDescriptorIF {
  private Field field;

  public FieldPropertyDescriptor(Class clazz, String fieldName, boolean isIndexed, int index,
      HintContainer srcDeepIndexHintContainer, HintContainer destDeepIndexHintContainer) {
    super(clazz, fieldName, isIndexed, index, srcDeepIndexHintContainer, destDeepIndexHintContainer);

    try {
      field = ReflectionUtils.getFieldFromBean(clazz, fieldName);
    } catch (NoSuchFieldException e) {
      MappingUtils.throwMappingException(e);
    }
    // Allow access to private instance var's that dont have public setter.
    field.setAccessible(true);
  }

  public Class getPropertyType() {
    return field.getType();
  }

  public Object getPropertyValue(Object bean) {
    Object result = null;
    Object o = null;
    try {
      o = field.get(bean);
    } catch (IllegalArgumentException e) {
      MappingUtils.throwMappingException(e);
    } catch (IllegalAccessException e) {
      MappingUtils.throwMappingException(e);
    }
    if (isIndexed) {
      result = MappingUtils.getIndexedValue(o, index);
    } else {
      result = o;
    }
    return result;
  }

  public void setPropertyValue(Object bean, Object value, FieldMap fieldMap) {
    if (getPropertyType().isPrimitive() && value == null) {
      // do nothing
      return;
    }

    // Check if dest value is already set and is equal to src value. If true, no need to rewrite the dest value
    try {
      if (getPropertyValue(bean) == value) {
        return;
      }
    } catch (Exception e) {
      // if we failed to read the value, assume we must write, and continue...
    }

    try {
      if (isIndexed) {
        Object existingValue = field.get(bean);
        field.set(bean, prepareIndexedCollection(existingValue, value));
      } else {
        field.set(bean, value);
      }
    } catch (IllegalAccessException e) {
      MappingUtils.throwMappingException(e);
    }
  }

}

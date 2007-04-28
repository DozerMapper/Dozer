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
package net.sf.dozer.util.mapping.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.beanutils.PropertyUtils;

import net.sf.dozer.util.mapping.MappingException;

/**
 * @author tierney.matt
 * @author garsombke.franz
 */
public class ReflectionUtils {

  public PropertyDescriptor findPropertyDescriptor(Class objectClass, String fieldName) {
    PropertyDescriptor result = null;

    if (fieldName.indexOf(MapperConstants.DEEP_FIELD_DELIMITOR) >= 0) {
      PropertyDescriptor[] hierarchy = getDeepFieldHierarchy(objectClass, fieldName);
      result = hierarchy[hierarchy.length - 1];
    } else {
      PropertyDescriptor[] descriptors = getPropertyDescriptors(objectClass);

      if (descriptors != null) {
        int size = descriptors.length;
        for (int i = 0; i < size; i++) {
          if (fieldName.equalsIgnoreCase(descriptors[i].getName())) {
            result = descriptors[i];
            break;
          }
        }
      }
    }

    return result;
  }

  public PropertyDescriptor[] getDeepFieldHierarchy(Class parentClass, String field) {
    if (field.indexOf(MapperConstants.DEEP_FIELD_DELIMITOR) < 0) {
      throw new MappingException("Field does not contain deep field delimitor");
    }

    StringTokenizer toks = new StringTokenizer(field, MapperConstants.DEEP_FIELD_DELIMITOR);
    Class latestClass = parentClass;
    PropertyDescriptor[] hierarchy = new PropertyDescriptor[toks.countTokens()];
    int index = 0;
    while (toks.hasMoreTokens()) {
      String aFieldName = toks.nextToken();
      PropertyDescriptor propDescriptor = findPropertyDescriptor(latestClass, aFieldName);

      if (propDescriptor == null) {
        throw new MappingException("Exception occurred determining deep field hierarchy for Class --> "
            + parentClass.getName() + ", Field --> " + field
            + ".  Unable to determine property descriptor for Class --> " + latestClass.getName() + ", Field Name: "
            + aFieldName);
      }

      latestClass = propDescriptor.getPropertyType();
      hierarchy[index++] = propDescriptor;
    }

    return hierarchy;
  }

  public Method getMethod(Object obj, String methodName) {
    Method[] methods = obj.getClass().getMethods();
    Method resultMethod = null;
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (method.getName().equals(methodName)) {
        resultMethod = method;
      }
    }
    if (resultMethod == null) {
      throw new MappingException("No method found for class:" + obj.getClass() + " and method name:" + methodName);
    }
    return resultMethod;
  }

  public Method findAMethod(Class parentDestClass, String methodName) throws NoSuchMethodException,
      ClassNotFoundException {
    // TODO USE HELPER from bean utils to find method w/ params
    StringTokenizer tokenizer = new StringTokenizer(methodName, "(");
    String m = tokenizer.nextToken();
    // If tokenizer has more elements, it mean that parameters may have been specified
    if (tokenizer.hasMoreElements()) {
      StringTokenizer tokens = new StringTokenizer(tokenizer.nextToken(), ")");
      String params = (tokens.hasMoreTokens() ? tokens.nextToken() : null);
      return findMethodWithParam(parentDestClass, m, params);
    }
    Method[] methods = parentDestClass.getMethods();
    Method result = null;
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (method.getName().equals(methodName)) {
        // Return the first method find
        return method;
      }
    }
    return result;
  }

  private Method findMethodWithParam(Class parentDestClass, String methodName, String params)
      throws NoSuchMethodException, ClassNotFoundException {
    // TODO USE HELPER from bean utils to find method w/ params
    List list = new ArrayList();
    if (params != null) {
      StringTokenizer tokenizer = new StringTokenizer(params, ",");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        list.add(Class.forName(token));
      }
    }
    return parentDestClass.getMethod(methodName, (Class[]) list.toArray(new Class[list.size()]));
  }

  protected PropertyDescriptor[] getPropertyDescriptors(Class objectClass) {
    // If the class is an interface, use custom method to get all prop descriptors in the inheritance hierarchy.
    // PropertyUtils.getPropertyDescriptors() does not work correctly for interface inheritance. It finds props in the
    // actual interface ok, but does not find props in the inheritance hierarchy.
    if (objectClass.isInterface()) {
      return getInterfacePropertyDescriptors(objectClass);
    } else {
      return PropertyUtils.getPropertyDescriptors(objectClass);
    }
  }

  private PropertyDescriptor[] getInterfacePropertyDescriptors(Class interfaceClass) {
    List propDescriptors = new ArrayList();
    // Add prop descriptors for interface passed in
    propDescriptors.addAll(Arrays.asList(PropertyUtils.getPropertyDescriptors(interfaceClass)));

    // Look for interface inheritance. If super interfaces are found, recurse up the hierarchy tree and add prop
    // descriptors for each interface found.
    // PropertyUtils.getPropertyDescriptors() does not correctly walk the inheritance hierarchy for interfaces.
    Class[] interfaces = interfaceClass.getInterfaces();
    if (interfaces != null) {
      for (int i = 0; i < interfaces.length; i++) {
        Class superInterfaceClass = interfaces[i];
        propDescriptors.addAll(Arrays.asList(getInterfacePropertyDescriptors(superInterfaceClass)));
      }
    }
    return (PropertyDescriptor[]) propDescriptors.toArray(new PropertyDescriptor[propDescriptors.size()]);
  }

  public Field getFieldFromBean(Class clazz, String fieldName) throws NoSuchFieldException {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      if (clazz.getSuperclass() != null) {
        return getFieldFromBean(clazz.getSuperclass(), fieldName);
      }
      throw e;
    }
  }

}
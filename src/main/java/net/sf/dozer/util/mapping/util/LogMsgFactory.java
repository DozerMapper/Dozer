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


import java.util.Collection;

import net.sf.dozer.util.mapping.fieldmap.FieldMap;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal class used to build various types of log messages.  Only intended for internal use.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 */
public abstract class LogMsgFactory {
  private static final Log log = LogFactory.getLog(LogMsgFactory.class);
  
  public static String createFieldMappingErrorMsg(Object srcObj, FieldMap fieldMapping, 
      Object destFieldValue, Object destObj, Throwable t) {
    String srcClassName = null;
    if (srcObj != null) {
      srcClassName = srcObj.getClass().getName();
    }

    String srcValueType = null;
    if (destFieldValue != null) {
      srcValueType = destFieldValue.getClass().toString();
    } else {
      srcValueType = fieldMapping.getSrcFieldType();
    }

    String destClassName = null;
    if (destObj != null) {
      destClassName = destObj.getClass().getName();
    }

    String destFieldTypeName = null;
    try {
      destFieldTypeName = fieldMapping.getDestFieldType(destObj.getClass()).getName();
    } catch (Exception e) {
      log.warn("unable to determine dest field type when build log.error message");
    }

    return "Field mapping error -->" + "\n  MapId: " + fieldMapping.getMapId() + "\n  Type: " + fieldMapping.getType()
        + "\n  Source parent class: " + srcClassName + "\n  Source field name: "
        + fieldMapping.getSrcFieldName() + "\n  Source field type: " + srcValueType
        + "\n  Source field value: " + destFieldValue + "\n  Dest parent class: " + destClassName
        + "\n  Dest field name: " + fieldMapping.getDestFieldName() + "\n  Dest field type: "
        + destFieldTypeName;
  }
  
  public static String createFieldMappingSuccessMsg(Class srcClass, Class destClass, String srcFieldName, String destFieldName,
    Object srcFieldValue, Object destFieldValue) {
    String srcClassStr = MappingUtils.getClassNameWithoutPackage(srcClass);
    String destClassStr = MappingUtils.getClassNameWithoutPackage(destClass);
      
    return "MAPPED: " + srcClassStr + "." + srcFieldName + " --> " + destClassStr + "." + destFieldName
        + "  VALUES: " + getLogOutput(srcFieldValue) + " --> "  + getLogOutput(destFieldValue);
  }
  
  private static String getLogOutput(Object object) {
    String output = "NULL";
    if (object == null) {
      return output;
    }
    try {
      if (object.getClass().isArray() || Collection.class.isAssignableFrom(object.getClass())) {
        output = ReflectionToStringBuilder.toString(object, ToStringStyle.MULTI_LINE_STYLE);
      } else {
        output = object.toString();
      }
    } catch (RuntimeException e) {
      output = object.toString();
    }
    return output;
  }
}
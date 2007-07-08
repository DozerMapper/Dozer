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
package net.sf.dozer.util.mapping.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import net.sf.dozer.util.mapping.util.DateFormatContainer;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BigIntegerConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.ByteConverter;
import org.apache.commons.beanutils.converters.CharacterConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.ShortConverter;

/**
 * Internal class for converting between wrapper types(including primitives). Only intended for internal use.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 * @author benson.matt
 */
public class PrimitiveOrWrapperConverter {
  private static final Map PRIMITIVE_TYPE_MAP = new HashMap();
  private static final Map CONVERTER_MAP = new HashMap();
  private static final Converter DEFAULT_CONVERTER = new StringConstructorConverter();

  static {
    // Set up PRIMITIVE_TYPE_MAP:
    Class[] primitives = { Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE };
    Class[] wrappers = { Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class,
        Double.class };
    for (int i = 0; i < primitives.length; i++) {
      PRIMITIVE_TYPE_MAP.put(primitives[i], wrappers[i]);
    }
    // Set up CONVERTER_MAP:
    CONVERTER_MAP.put(Integer.class, new IntegerConverter());
    CONVERTER_MAP.put(Double.class, new DoubleConverter());
    CONVERTER_MAP.put(Short.class, new ShortConverter());
    CONVERTER_MAP.put(Character.class, new CharacterConverter());
    CONVERTER_MAP.put(Long.class, new LongConverter());
    CONVERTER_MAP.put(Boolean.class, new BooleanConverter());
    CONVERTER_MAP.put(Byte.class, new ByteConverter());
    CONVERTER_MAP.put(Float.class, new FloatConverter());
    CONVERTER_MAP.put(BigDecimal.class, new BigDecimalConverter());
    CONVERTER_MAP.put(BigInteger.class, new BigIntegerConverter());
  }

  public Object convert(Object srcFieldValue, Class destFieldClass, DateFormatContainer dateFormatContainer) {
    if (srcFieldValue == null || destFieldClass == null || (srcFieldValue.equals("") && !destFieldClass.equals(String.class))) {
      return null;
    }
    Converter converter = getPrimitiveOrWrapperConverter(destFieldClass, dateFormatContainer);
    try {
      return converter.convert(destFieldClass, srcFieldValue);
    } catch (org.apache.commons.beanutils.ConversionException e) {
      throw new net.sf.dozer.util.mapping.converters.ConversionException(e);
    }
  }

  private Converter getPrimitiveOrWrapperConverter(Class destClass, DateFormatContainer dateFormatContainer) {
    if (String.class.equals(destClass)) {
      return new StringConverter(dateFormatContainer);
    }

    Converter result = (Converter) CONVERTER_MAP.get(destClass.isPrimitive() ? wrapPrimitive(destClass) : destClass);

    if (result == null) {
      if (java.util.Date.class.isAssignableFrom(destClass)) {
        result = new DateConverter(dateFormatContainer.getDateFormat());
      }
      if (Calendar.class.isAssignableFrom(destClass)) {
        result = new CalendarConverter(dateFormatContainer.getDateFormat());
      }
    }
    return result == null ? DEFAULT_CONVERTER : result;
  }

  private Class wrapPrimitive(Class c) {
    return (Class) PRIMITIVE_TYPE_MAP.get(c);
  }

}
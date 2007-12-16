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
package net.sf.dozer.util.mapping;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.dozer.util.mapping.cache.Cache;
import net.sf.dozer.util.mapping.cache.CacheEntry;
import net.sf.dozer.util.mapping.cache.CacheKeyFactory;
import net.sf.dozer.util.mapping.cache.CacheManagerIF;
import net.sf.dozer.util.mapping.classmap.ClassMap;
import net.sf.dozer.util.mapping.classmap.Configuration;
import net.sf.dozer.util.mapping.config.GlobalSettings;
import net.sf.dozer.util.mapping.converters.CustomConverter;
import net.sf.dozer.util.mapping.converters.PrimitiveOrWrapperConverter;
import net.sf.dozer.util.mapping.event.DozerEvent;
import net.sf.dozer.util.mapping.event.DozerEventManager;
import net.sf.dozer.util.mapping.event.EventManagerIF;
import net.sf.dozer.util.mapping.fieldmap.CustomGetSetMethodFieldMap;
import net.sf.dozer.util.mapping.fieldmap.ExcludeFieldMap;
import net.sf.dozer.util.mapping.fieldmap.FieldMap;
import net.sf.dozer.util.mapping.fieldmap.HintContainer;
import net.sf.dozer.util.mapping.fieldmap.MapFieldMap;
import net.sf.dozer.util.mapping.stats.StatisticTypeConstants;
import net.sf.dozer.util.mapping.stats.StatisticsManagerIF;
import net.sf.dozer.util.mapping.util.ClassMapBuilder;
import net.sf.dozer.util.mapping.util.ClassMapFinder;
import net.sf.dozer.util.mapping.util.ClassMapKeyFactory;
import net.sf.dozer.util.mapping.util.CollectionUtils;
import net.sf.dozer.util.mapping.util.DateFormatContainer;
import net.sf.dozer.util.mapping.util.DestBeanCreator;
import net.sf.dozer.util.mapping.util.Jdk5Methods;
import net.sf.dozer.util.mapping.util.LogMsgFactory;
import net.sf.dozer.util.mapping.util.MapperConstants;
import net.sf.dozer.util.mapping.util.MappingUtils;
import net.sf.dozer.util.mapping.util.MappingValidator;
import net.sf.dozer.util.mapping.util.ReflectionUtils;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal Mapping Engine. Not intended for direct use by Application code.
 * This class does most of the heavy lifting and is very recursive in nature.
 * 
 * @author garsombke.franz
 * @author sullins.ben
 * @author tierney.matt
 * 
 */
public class MappingProcessor implements MapperIF {

  private static final Log log = LogFactory.getLog(MappingProcessor.class);

  private final Map customMappings;
  private final Configuration globalConfiguration;
  private final List customConverterObjects;// actual converter object instances
  private final Map customConverterObjectsWithId; // key/value pair of custom converter id and object instance
  private final StatisticsManagerIF statsMgr;
  private final EventManagerIF eventMgr;
  private final CustomFieldMapperIF customFieldMapper;
  private final Map mappedFields = new HashMap();
  private final Cache converterByDestTypeCache;
  private final Cache superTypeCache;
  private final PrimitiveOrWrapperConverter primitiveOrWrapperConverter = new PrimitiveOrWrapperConverter();

  protected MappingProcessor(Map customMappings, Configuration globalConfiguration, CacheManagerIF cacheMgr,
      StatisticsManagerIF statsMgr, List customConverterObjects, List eventListeners, CustomFieldMapperIF customFieldMapper,
      Map customConverterObjectsWithId) {
    this.customMappings = customMappings;
    this.globalConfiguration = globalConfiguration;
    this.statsMgr = statsMgr;
    this.customConverterObjects = customConverterObjects;
    this.eventMgr = new DozerEventManager(eventListeners);
    this.customFieldMapper = customFieldMapper;
    this.converterByDestTypeCache = cacheMgr.getCache(MapperConstants.CONVERTER_BY_DEST_TYPE_CACHE);
    this.superTypeCache = cacheMgr.getCache(MapperConstants.SUPER_TYPE_CHECK_CACHE);
    this.customConverterObjectsWithId = customConverterObjectsWithId;
  }

  public Object map(Object srcObj, Class destClass) {
    // map-id is optional, so just pass in null
    return map(srcObj, destClass, (String) null);
  }

  public Object map(Object srcObj, Class destClass, String mapId) {
    MappingValidator.validateMappingRequest(srcObj, destClass);

    Object result = null;
    ClassMap classMap = null;
    try {
      classMap = getClassMap(srcObj, destClass, mapId);

      // Check to see if custom converter has been specified for this mapping
      // combination. If so, just use it.
      Class converterClass = MappingUtils.findCustomConverter(converterByDestTypeCache, classMap.getCustomConverters(), srcObj
          .getClass(), destClass);
      if (converterClass != null) {
        eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_STARTED_EVENT, classMap, null, srcObj, result, null));
        return mapUsingCustomConverter(converterClass, srcObj.getClass(), srcObj, destClass, result, null, true);
      }

      // Create destination object. It will be populated in the call to map()
      result = DestBeanCreator.create(srcObj, classMap.getSrcClassToMap(), classMap.getDestClassToMap(), destClass, classMap
          .getDestClassBeanFactory(), classMap.getDestClassBeanFactoryId(), classMap.getDestClassCreateMethod());

      // Map src values to dest object
      eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_STARTED_EVENT, classMap, null, srcObj, result, null));
      map(classMap, srcObj, result, false, null);
    } catch (Throwable e) {
      MappingUtils.throwMappingException(e);
    }
    eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_FINISHED_EVENT, classMap, null, srcObj, result, null));
    return result;
  }

  public void map(Object srcObj, Object destObj) {
    map(srcObj, destObj, null);
  }

  public void map(Object srcObj, Object destObj, String mapId) {
    MappingValidator.validateMappingRequest(srcObj, destObj);

    ClassMap classMap = null;
    try {
      classMap = getClassMap(srcObj, destObj.getClass(), mapId);

      // Check to see if custom converter has been specified for this mapping
      // combination. If so, just use it.
      Class converterClass = MappingUtils.findCustomConverter(converterByDestTypeCache, classMap.getCustomConverters(), srcObj
          .getClass(), destObj.getClass());

      if (converterClass != null) {
        eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_STARTED_EVENT, classMap, null, srcObj, destObj, null));
        mapUsingCustomConverter(converterClass, srcObj.getClass(), srcObj, destObj.getClass(), destObj, null, true);
        return;
      }

      // Map src values to dest object
      eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_STARTED_EVENT, classMap, null, srcObj, destObj, null));
      map(classMap, srcObj, destObj, false, null);
    } catch (Throwable e) {
      MappingUtils.throwMappingException(e);
    }
    eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_FINISHED_EVENT, classMap, null, srcObj, destObj, null));
  }

  private void map(ClassMap classMap, Object srcObj, Object destObj, boolean bypassSuperMappings, String mapId) {
    // 1596766 - Recursive object mapping issue. Prevent recursive mapping
    // infinite loop. Keep a record of mapped fields
    // by storing the id of the sourceObj and the destObj to be mapped. This can
    // be referred to later to avoid recursive mapping loops
    mappedFields.put(srcObj, destObj);

    // If class map hasnt already been determined, find the appropriate one for
    // the src/dest object combination
    if (classMap == null) {
      classMap = getClassMap(srcObj, destObj.getClass(), mapId);
    }

    Class srcClass = srcObj.getClass();
    Class destClass = destObj.getClass();

    // Check to see if custom converter has been specified for this mapping
    // combination. If so, just use it.
    Class converterClass = MappingUtils.findCustomConverter(converterByDestTypeCache, classMap.getCustomConverters(), srcClass,
        destClass);
    if (converterClass != null) {
      mapUsingCustomConverter(converterClass, srcClass, srcObj, destClass, destObj, null, true);
      return;
    }

    // Now check for super class mappings.  Process super class mappings first.
    List mappedParentFields = null;
    if (!bypassSuperMappings) {
      // check for super classes
      Set superClasses = checkForSuperTypeMapping(srcClass, destClass);
      // check for interfaces
      superClasses.addAll(ClassMapFinder.findInterfaceMappings(this.customMappings, srcClass, destClass));
      if (superClasses.size() > 0) {
        mappedParentFields = processSuperTypeMapping(superClasses, srcObj, destObj, mapId);
      }
    }

    // Perform mappings for each field. Iterate through Fields Maps for this class mapping
    List fieldMaps = classMap.getFieldMaps();
    int size = fieldMaps.size();
    for (int i = 0; i < size; i++) {
      FieldMap fieldMapping = (FieldMap) fieldMaps.get(i);

      //Bypass field if it has already been mapped as part of super class mappings.
      String key = MappingUtils.getMappedParentFieldKey(destObj, fieldMapping.getDestFieldName());
      if (mappedParentFields != null && mappedParentFields.contains(key)) {
        continue;
      }

      mapField(fieldMapping, srcObj, destObj);
    }
  }

  private void mapField(FieldMap fieldMapping, Object srcObj, Object destObj) {

    // The field has been explicitly excluded from mapping. So just return, as
    // no further processing is needed for this field
    if (fieldMapping instanceof ExcludeFieldMap) {
      return;
    }

    Object srcFieldValue = null;
    try {
      // If a custom field mapper was specified, then invoke it. If not, or the
      // custom field mapper returns false(indicating the
      // field was not actually mapped by the custom field mapper), proceed as
      // normal(use Dozer to map the field)
      srcFieldValue = fieldMapping.getSrcFieldValue(srcObj);
      boolean fieldMapped = false;
      if (customFieldMapper != null) {
        fieldMapped = customFieldMapper.mapField(srcObj, destObj, srcFieldValue, fieldMapping.getClassMap(), fieldMapping);
      }

      if (!fieldMapped) {
        if (fieldMapping.getDestFieldType() != null && fieldMapping.getDestFieldType().equals(MapperConstants.ITERATE)) {
          // special logic for iterate feature
          mapFromIterateMethodFieldMap(srcObj, destObj, srcFieldValue, fieldMapping);
        } else {
          // either deep field map or generic map. The is the most likely
          // scenario
          mapFromFieldMap(srcObj, destObj, srcFieldValue, fieldMapping);
        }
      }

      statsMgr.increment(StatisticTypeConstants.FIELD_MAPPING_SUCCESS_COUNT);

    } catch (Throwable e) {
      log.error(LogMsgFactory.createFieldMappingErrorMsg(srcObj, fieldMapping, srcFieldValue, destObj, e), e);
      statsMgr.increment(StatisticTypeConstants.FIELD_MAPPING_FAILURE_COUNT);

      // check error handling policy.
      if (fieldMapping.isStopOnErrors()) {
        MappingUtils.throwMappingException(e);
      } else {
        // check if any Exceptions should be allowed to be thrown
        if (!fieldMapping.getClassMap().getAllowedExceptions().isEmpty() && e.getCause() instanceof InvocationTargetException) {
          Throwable thrownType = ((InvocationTargetException) e.getCause()).getTargetException();
          Class exceptionClass = thrownType.getClass();
          if (fieldMapping.getClassMap().getAllowedExceptions().contains(exceptionClass)) {
            throw (RuntimeException) thrownType;
          }
        }
        statsMgr.increment(StatisticTypeConstants.FIELD_MAPPING_FAILURE_IGNORED_COUNT);
      }
    }
  }

  private void mapFromFieldMap(Object srcObj, Object destObj, Object srcFieldValue, FieldMap fieldMapping) {
    Class destFieldType = null;
    if (fieldMapping instanceof CustomGetSetMethodFieldMap) {
      try {
        destFieldType = fieldMapping.getDestFieldWriteMethod(destObj.getClass()).getParameterTypes()[0];
      } catch (Throwable e) {
        // try traditional way
        destFieldType = fieldMapping.getDestFieldType(destObj.getClass());
      }
    } else {
      destFieldType = fieldMapping.getDestFieldType(destObj.getClass());
    }

    // 1476780 - 12/2006 mht - Add support for field level custom converters
    // Use field level custom converter if one was specified. Otherwise, map or
    // recurse the object as normal
    // 1770440 - fdg - Using multiple instances of CustomConverter
    Object destFieldValue = null;
    if (!MappingUtils.isBlankOrNull(fieldMapping.getCustomConverterId())) {
      if (customConverterObjectsWithId != null && customConverterObjectsWithId.containsKey(fieldMapping.getCustomConverterId())) {
        Class srcFieldClass = srcFieldValue != null ? srcFieldValue.getClass() : fieldMapping.getSrcFieldType(srcObj.getClass());
        destFieldValue = mapUsingCustomConverterInstance(customConverterObjectsWithId.get(fieldMapping.getCustomConverterId()),
            srcFieldClass, srcFieldValue, destFieldType, destObj, fieldMapping, false);
      } else {
        throw new MappingException("CustomConverter instance not found with id:" + fieldMapping.getCustomConverterId());
      }
    } else if (MappingUtils.isBlankOrNull(fieldMapping.getCustomConverter())) {
      destFieldValue = mapOrRecurseObject(srcObj, srcFieldValue, destFieldType, fieldMapping, destObj);
    } else {
      Class srcFieldClass = srcFieldValue != null ? srcFieldValue.getClass() : fieldMapping.getSrcFieldType(srcObj.getClass());
      destFieldValue = mapUsingCustomConverter(MappingUtils.loadClass(fieldMapping.getCustomConverter()), srcFieldClass,
          srcFieldValue, destFieldType, destObj, fieldMapping, false);
    }

    writeDestinationValue(destObj, destFieldValue, fieldMapping, srcObj);

    if (log.isDebugEnabled()) {
      log.debug(LogMsgFactory.createFieldMappingSuccessMsg(srcObj.getClass(), destObj.getClass(), fieldMapping.getSrcFieldName(),
          fieldMapping.getDestFieldName(), srcFieldValue, destFieldValue, fieldMapping.getClassMap().getMapId()));
    }
  }

  private Object mapOrRecurseObject(Object srcObj, Object srcFieldValue, Class destFieldType, FieldMap fieldMap, Object destObj) {
    Class srcFieldClass = srcFieldValue != null ? srcFieldValue.getClass() : fieldMap.getSrcFieldType(srcObj.getClass());
    Class converterClass = MappingUtils.determineCustomConverter(fieldMap, converterByDestTypeCache, fieldMap.getClassMap()
        .getCustomConverters(), srcFieldClass, destFieldType);

    // 1-2007 mht: Invoke custom converter even if the src value is null.
    // #1563795
    if (converterClass != null) {
      return mapUsingCustomConverter(converterClass, srcFieldClass, srcFieldValue, destFieldType, destObj, fieldMap, false);
    }

    if (srcFieldValue == null) {
      return null;
    }

    // 1596766 - Recursive object mapping issue. Prevent recursive mapping
    // infinite loop
    Object alreadyMappedValue = null;
    if (srcFieldValue != null) {
      alreadyMappedValue = mappedFields.get(srcFieldValue);
    }
    if (alreadyMappedValue != null) {
      // 1664984 - bi-directionnal mapping with sets & subclasses
      if (destFieldType.isAssignableFrom(alreadyMappedValue.getClass())) {
        // Source value has already been mapped to the required destFieldType.
        return alreadyMappedValue;
      }

      // 1658168 - Recursive mapping issue for interfaces
      if (destFieldType.isInterface() && destFieldType.isAssignableFrom(alreadyMappedValue.getClass())) {
        // Source value has already been mapped to the required destFieldType.
        return alreadyMappedValue;
      }
    }

    if (fieldMap.isCopyByReference()) {
      // just get the src and return it, no transformation.
      return srcFieldValue;
    }

    boolean isSrcFieldClassSupportedMap = MappingUtils.isSupportedMap(srcFieldClass);
    boolean isDestFieldTypeSupportedMap = MappingUtils.isSupportedMap(destFieldType);
    if (isSrcFieldClassSupportedMap && isDestFieldTypeSupportedMap) {
      return mapMap(srcObj, srcFieldValue, fieldMap, destObj);
    }
    if (fieldMap instanceof MapFieldMap && destFieldType.equals(Object.class)) {
      // TODO: find better place for this logic. try to encapsulate in FieldMap?
      destFieldType = fieldMap.getDestHintContainer() != null ? fieldMap.getDestHintContainer().getHint() : srcFieldClass;
    }

    if (MappingUtils.isPrimitiveOrWrapper(srcFieldClass) || MappingUtils.isPrimitiveOrWrapper(destFieldType)) {
      // Primitive or Wrapper conversion
      if (fieldMap.getDestHintContainer() != null) {
        destFieldType = fieldMap.getDestHintContainer().getHint();
      }
      DateFormatContainer dfContainer = new DateFormatContainer(fieldMap.getDateFormat());

      //#1841448 - if trim-strings=true, then use a trimmed src string value when converting to dest value
      Object convertSrcFieldValue = srcFieldValue;
      if (srcFieldValue != null && fieldMap.isTrimStrings() && srcFieldValue.getClass().equals(String.class)) {
        convertSrcFieldValue = ((String) srcFieldValue).trim();
      }

      if (fieldMap instanceof MapFieldMap && !MappingUtils.isPrimitiveOrWrapper(destFieldType)) {
        // This handles a very special/rare use case(see indexMapping.xml + unit
        // test
        // testStringToIndexedSet_UsingMapSetMethod). If the destFieldType is a
        // custom object AND has a String param
        // constructor, we don't want to construct the custom object with the
        // src value because the map backed property
        // logic at lower layers handles setting the value on the custom object.
        // Without this special logic, the
        // destination map backed custom object would contain a value that is
        // the custom object dest type instead of the
        // desired src value.
        return primitiveOrWrapperConverter.convert(convertSrcFieldValue, convertSrcFieldValue.getClass(), dfContainer);
      } else {
        return primitiveOrWrapperConverter.convert(convertSrcFieldValue, destFieldType, dfContainer);
      }
    }
    if (MappingUtils.isSupportedCollection(srcFieldClass) && (MappingUtils.isSupportedCollection(destFieldType))) {
      return mapCollection(srcObj, srcFieldValue, fieldMap, destObj);
    }
    if (GlobalSettings.getInstance().isJava5()
        && ((Boolean) ReflectionUtils.invoke(Jdk5Methods.getInstance().getClassIsEnumMethod(), srcFieldClass, null)).booleanValue()
        && ((Boolean) ReflectionUtils.invoke(Jdk5Methods.getInstance().getClassIsEnumMethod(), destFieldType, null)).booleanValue()) {
      return mapEnum(srcFieldValue, destFieldType);
    }

    // Default: Map from one custom data object to another custom data object
    return mapCustomObject(fieldMap, destObj, destFieldType, srcFieldValue);
  }

  private Object mapEnum(Object srcFieldValue, Class destFieldType) {
    String name = (String) ReflectionUtils.invoke(Jdk5Methods.getInstance().getEnumNameMethod(), srcFieldValue, null);
    return ReflectionUtils.invoke(Jdk5Methods.getInstance().getEnumValueOfMethod(), destFieldType, new Object[] { destFieldType,
        name });
  }

  private Object mapCustomObject(FieldMap fieldMap, Object destObj, Class destFieldType, Object srcFieldValue) {
    // Custom java bean. Need to make sure that the destination object is not
    // already instantiated.
    Object result = getExistingValue(fieldMap, destObj, destFieldType);
    ClassMap classMap = null;
    // if the field is not null than we don't want a new instance
    if (result == null) {
      // first check to see if this plain old field map has hints to the actual
      // type.
      if (fieldMap.getDestHintContainer() != null) {
        Class destHintType = fieldMap.getDestHintContainer().getHint();
        // if the destType is null this means that there was more than one hint.
        // we must have already set the destType then.
        if (destHintType != null) {
          destFieldType = destHintType;
        }
      }
      // Check to see if explicit map-id has been specified for the field
      // mapping
      String mapId = fieldMap.getMapId();
      classMap = getClassMap(srcFieldValue, destFieldType, mapId);

      result = DestBeanCreator.create(srcFieldValue, classMap.getSrcClassToMap(),
          fieldMap.getDestHintContainer() != null ? fieldMap.getDestHintContainer().getHint() : classMap.getDestClassToMap(),
          destFieldType, classMap.getDestClassBeanFactory(), classMap.getDestClassBeanFactoryId(), fieldMap
              .getDestFieldCreateMethod() != null ? fieldMap.getDestFieldCreateMethod() : classMap.getDestClassCreateMethod());
    }

    map(classMap, srcFieldValue, result, false, fieldMap.getMapId());

    return result;
  }

  private Object mapCollection(Object srcObj, Object srcCollectionValue, FieldMap fieldMap, Object destObj) {
    // since we are mapping some sort of collection now is a good time to decide
    // if they provided hints
    // if no hint is provided then we will check to see if we can use JDK 1.5
    // generics to determine the mapping type
    // this will only happen once on the dest hint. the next mapping will
    // already have the hint
    if (fieldMap.getDestHintContainer() == null && GlobalSettings.getInstance().isJava5()) {
      Class genericType = null;
      try {
        Method method = fieldMap.getDestFieldWriteMethod(destObj.getClass());
        genericType = ReflectionUtils.determineGenericsType(method, false);
      } catch (Throwable e) {
        log.info("The destObj:" + destObj + " does not have a write method");
      }
      if (genericType != null) {
        HintContainer destHintContainer = new HintContainer();
        destHintContainer.setHintName(genericType.getName());
        fieldMap.setDestHintContainer(destHintContainer);
      }
    }

    // if it is an iterator object turn it into a List
    if (srcCollectionValue instanceof Iterator) {
      srcCollectionValue = IteratorUtils.toList((Iterator) srcCollectionValue);
    }

    Class destCollectionType = fieldMap.getDestFieldType(destObj.getClass());
    Class srcFieldType = srcCollectionValue.getClass();
    Object result = null;

    // if they use a standard Collection we have to assume it is a List...better
    // way to handle this?
    if (destCollectionType.getName().equals(Collection.class.getName())) {
      destCollectionType = List.class;
    }
    // Array to Array
    if (CollectionUtils.isArray(srcFieldType) && (CollectionUtils.isArray(destCollectionType))) {
      result = mapArrayToArray(srcObj, srcCollectionValue, fieldMap, destObj);
      // Array to List
    } else if (CollectionUtils.isArray(srcFieldType) && (CollectionUtils.isList(destCollectionType))) {
      result = mapArrayToList(srcObj, srcCollectionValue, fieldMap, destObj);
    }
    // List to Array
    else if (CollectionUtils.isList(srcFieldType) && (CollectionUtils.isArray(destCollectionType))) {
      result = mapListToArray(srcObj, (List) srcCollectionValue, fieldMap, destObj);
      // List to List
    } else if (CollectionUtils.isList(srcFieldType) && (CollectionUtils.isList(destCollectionType))) {
      result = mapListToList(srcObj, (List) srcCollectionValue, fieldMap, destObj);
    }
    // Set to Array
    else if (CollectionUtils.isSet(srcFieldType) && CollectionUtils.isArray(destCollectionType)) {
      result = mapSetToArray(srcObj, (Set) srcCollectionValue, fieldMap, destObj);
    }
    // Array to Set
    else if (CollectionUtils.isArray(srcFieldType) && CollectionUtils.isSet(destCollectionType)) {
      result = addToSet(srcObj, fieldMap, Arrays.asList((Object[]) srcCollectionValue), destObj);
    }
    // Set to List
    else if (CollectionUtils.isSet(srcFieldType) && CollectionUtils.isList(destCollectionType)) {
      result = mapListToList(srcObj, (Set) srcCollectionValue, fieldMap, destObj);
    }
    // Collection to Set
    else if (CollectionUtils.isCollection(srcFieldType) && CollectionUtils.isSet(destCollectionType)) {
      result = addToSet(srcObj, fieldMap, (Collection) srcCollectionValue, destObj);
    }
    return result;
  }

  private Object mapMap(Object srcObj, Object srcMapValue, FieldMap fieldMap, Object destObj) {
    Map result = null;
    Object field = fieldMap.getDestValue(destObj);
    if (field == null) {
      // no destination map exists
      result = (Map) DestBeanCreator.create(srcMapValue.getClass());
    } else {
      result = (Map) field;
    }
    Map srcMap = (Map) srcMapValue;
    Set srcEntrySet = srcMap.entrySet();
    Iterator iter = srcEntrySet.iterator();
    while (iter.hasNext()) {
      Map.Entry srcEntry = (Map.Entry) iter.next();
      Object srcEntryValue = srcEntry.getValue();
      Object destEntryValue = mapOrRecurseObject(srcObj, srcEntryValue, srcEntryValue.getClass(), fieldMap, destObj);
      Object obj = result.get(srcEntry.getKey());
      if (obj != null && obj.equals(destEntryValue)
          && MapperConstants.RELATIONSHIP_NON_CUMULATIVE.equals(fieldMap.getRelationshipType())) {
        map(null, srcEntryValue, obj, false, null);
      } else {
        result.put(srcEntry.getKey(), destEntryValue);
      }
    }
    return result;
  }

  private Object mapArrayToArray(Object srcObj, Object srcCollectionValue, FieldMap fieldMap, Object destObj) {
    Class destEntryType = fieldMap.getDestFieldType(destObj.getClass()).getComponentType();
    int size = Array.getLength(srcCollectionValue);
    if (CollectionUtils.isPrimitiveArray(srcCollectionValue.getClass())) {
      return addToPrimitiveArray(srcObj, fieldMap, size, srcCollectionValue, destObj, destEntryType);
    } else {
      List list = Arrays.asList((Object[]) srcCollectionValue);
      List returnList = null;
      if (!destEntryType.getName().equals("java.lang.Object")) {
        returnList = addOrUpdateToList(srcObj, fieldMap, list, destObj, destEntryType);
      } else {
        returnList = addOrUpdateToList(srcObj, fieldMap, list, destObj, null);
      }
      return CollectionUtils.convertListToArray(returnList, destEntryType);
    }
  }

  private void mapFromIterateMethodFieldMap(Object srcObj, Object destObj, Object srcFieldValue, FieldMap fieldMapping) {
    // Iterate over the destFieldValue - iterating is fine unless we are mapping
    // in the other direction.
    // Verify that it is truly a collection if it is an iterator object turn it
    // into a List
    if (srcFieldValue instanceof Iterator) {
      srcFieldValue = IteratorUtils.toList((Iterator) srcFieldValue);
    }
    if (srcFieldValue != null) {
      for (int i = 0; i < CollectionUtils.getLengthOfCollection(srcFieldValue); i++) {
        Object value = CollectionUtils.getValueFromCollection(srcFieldValue, i);
        // map this value
        if (fieldMapping.getDestHintContainer() == null) {
          MappingUtils.throwMappingException("<field type=\"iterate\"> must have a source or destination type hint");
        }
        // check for custom converters
        Class converterClass = MappingUtils.findCustomConverter(converterByDestTypeCache, fieldMapping.getClassMap()
            .getCustomConverters(), value.getClass(), fieldMapping.getDestHintContainer().getHint());

        if (converterClass != null) {
          Class srcFieldClass = srcFieldValue.getClass();
          value = mapUsingCustomConverter(converterClass, srcFieldClass, value, fieldMapping.getDestHintContainer().getHint(),
              null, fieldMapping, false);
        } else {
          value = map(value, fieldMapping.getDestHintContainer().getHint());
        }
        if (value != null) {
          writeDestinationValue(destObj, value, fieldMapping, srcObj);
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(LogMsgFactory.createFieldMappingSuccessMsg(srcObj.getClass(), destObj.getClass(), fieldMapping.getSrcFieldName(),
          fieldMapping.getDestFieldName(), srcFieldValue, null, fieldMapping.getClassMap().getMapId()));
    }
  }

  private Object addToPrimitiveArray(Object srcObj, FieldMap fieldMap, int size, Object srcCollectionValue, Object destObj,
      Class destEntryType) {

    Object result = null;
    Object field = fieldMap.getDestValue(destObj);
    int arraySize = 0;
    if (field == null) {
      result = Array.newInstance(destEntryType, size);
    } else {
      result = Array.newInstance(destEntryType, size + Array.getLength(field));
      arraySize = Array.getLength(field);
      for (int i = 0; i < Array.getLength(field); i++) {
        Array.set(result, i, Array.get(field, i));
      }
    }
    // primitive arrays are ALWAYS cumulative
    for (int i = 0; i < size; i++) {
      Object toValue = mapOrRecurseObject(srcObj, Array.get(srcCollectionValue, i), destEntryType, fieldMap, destObj);
      Array.set(result, arraySize, toValue);
      arraySize++;
    }
    return result;
  }

  private Object mapListToArray(Object srcObj, Collection srcCollectionValue, FieldMap fieldMap, Object destObj) {
    List list = null;
    Class destEntryType = fieldMap.getDestFieldType(destObj.getClass()).getComponentType();

    if (!destEntryType.getName().equals("java.lang.Object")) {
      list = addOrUpdateToList(srcObj, fieldMap, srcCollectionValue, destObj, destEntryType);
    } else {
      list = addOrUpdateToList(srcObj, fieldMap, srcCollectionValue, destObj);
    }
    return CollectionUtils.convertListToArray(list, destEntryType);
  }

  private List mapListToList(Object srcObj, Collection srcCollectionValue, FieldMap fieldMap, Object destObj) {
    return addOrUpdateToList(srcObj, fieldMap, srcCollectionValue, destObj);
  }

  private Set addToSet(Object srcObj, FieldMap fieldMap, Collection srcCollectionValue, Object destObj) {
    // create a list here so we can keep track of which elements we have mapped, and remove all others if removeOrphans = true
    List mappedElements = new ArrayList();
    Class destEntryType = null;

    ListOrderedSet result = new ListOrderedSet();
    // don't want to create the set if it already exists.
    Object field = fieldMap.getDestValue(destObj);
    if (field != null) {
      result.addAll((Collection) field);
    }
    Object destValue = null;
    Iterator iter = srcCollectionValue.iterator();
    Object srcValue = null;
    while (iter.hasNext()) {
      srcValue = iter.next();
      if (destEntryType == null
          || (fieldMap.getDestHintContainer() != null && fieldMap.getDestHintContainer().hasMoreThanOneHint())) {
        destEntryType = fieldMap.getDestHintType(srcValue.getClass());
      }
      destValue = mapOrRecurseObject(srcObj, srcValue, destEntryType, fieldMap, destObj);
      if (fieldMap.getRelationshipType() == null || fieldMap.getRelationshipType().equals(MapperConstants.RELATIONSHIP_CUMULATIVE)) {
        result.add(destValue);
        mappedElements.add(destValue);

      } else {
        if (result.contains(destValue)) {
          int index = result.indexOf(destValue);
          // perform an update if complex type - can't map strings
          Object obj = result.get(index);
          // make sure it is not a String
          if (!obj.getClass().isAssignableFrom(String.class)) {
            map(null, srcValue, obj, false, null);
            mappedElements.add(obj);
          }
        } else {
          result.add(destValue);
          mappedElements.add(destValue);
        }
      }
    }

    // If remove orphans - we only want to keep the objects we've mapped from the src collection
    // so we'll clear result and replace all entries with the ones in mappedElements
    if (fieldMap.isRemoveOrphans()) {
      result.clear();
      result.addAll(mappedElements);
    }

    if (field == null) {
      Class destSetType = fieldMap.getDestFieldType(destObj.getClass());
      return CollectionUtils.createNewSet(destSetType, result);
    } else {
      // Bug #1822421 - Clear first so we don't end up with the removed orphans again
      ((Set) field).clear();
      ((Set) field).addAll(result);
      return (Set) field;
    }
  }

  private List addOrUpdateToList(Object srcObj, FieldMap fieldMap, Collection srcCollectionValue, Object destObj,
      Class destEntryType) {
    // create a Set here so we can keep track of which elements we have mapped, and remove all others if removeOrphans = true
    Set mappedElements = new HashSet();
    List result = null;
    // don't want to create the list if it already exists.
    // these maps are special cases which do not fall under what we are looking
    // for
    Object field = fieldMap.getDestValue(destObj);
    if (field == null) {
      result = new ArrayList(srcCollectionValue.size());
    } else {
      if (CollectionUtils.isList(field.getClass())) {
        result = (List) field;
      } else if (CollectionUtils.isArray(field.getClass())) {// must be array
        result = new ArrayList(Arrays.asList((Object[]) field));
      } else { // assume it is neither - safest way is to create new
        // List
        result = new ArrayList(srcCollectionValue.size());
      }
    }
    Object destValue = null;
    Iterator iter = srcCollectionValue.iterator();
    Object srcValue = null;
    Class prevDestEntryType = null;
    while (iter.hasNext()) {
      srcValue = iter.next();
      if (destEntryType == null
          || (fieldMap.getDestHintContainer() != null && fieldMap.getDestHintContainer().hasMoreThanOneHint())) {
        if (srcValue == null) {
          destEntryType = prevDestEntryType;
        } else {
          destEntryType = fieldMap.getDestHintType(srcValue.getClass());
        }
      }
      destValue = mapOrRecurseObject(srcObj, srcValue, destEntryType, fieldMap, destObj);
      prevDestEntryType = destEntryType;
      if (fieldMap.getRelationshipType() == null || fieldMap.getRelationshipType().equals(MapperConstants.RELATIONSHIP_CUMULATIVE)) {
        result.add(destValue);
        mappedElements.add(destValue);
      } else {
        if (result.contains(destValue)) {
          int index = result.indexOf(destValue);
          // perform an update if complex type - can't map strings
          Object obj = result.get(index);
          // make sure it is not a String
          if (!obj.getClass().isAssignableFrom(String.class)) {
            map(null, srcValue, obj, false, null);
            mappedElements.add(obj);
          }
        } else {
          result.add(destValue);
          mappedElements.add(destValue);
        }
      }
    }

    // If remove orphans - we only want to keep the objects we've mapped from the src collection
    // so we'll clear result and replace all entries with the ones in mappedElements
    if (fieldMap.isRemoveOrphans()) {
      result.clear();
      result.addAll(mappedElements);
    }

    return result;
  }

  private List addOrUpdateToList(Object srcObj, FieldMap fieldMap, Collection srcCollectionValue, Object destObj) {
    return addOrUpdateToList(srcObj, fieldMap, srcCollectionValue, destObj, null);
  }

  private Object mapSetToArray(Object srcObj, Collection srcCollectionValue, FieldMap fieldMap, Object destObj) {
    return mapListToArray(srcObj, srcCollectionValue, fieldMap, destObj);
  }

  private List mapArrayToList(Object srcObj, Object srcCollectionValue, FieldMap fieldMap, Object destObj) {
    Class destEntryType = null;
    if (fieldMap.getDestHintContainer() != null) {
      destEntryType = fieldMap.getDestHintContainer().getHint();
    } else {
      destEntryType = srcCollectionValue.getClass().getComponentType();
    }
    List srcValueList = null;
    if (CollectionUtils.isPrimitiveArray(srcCollectionValue.getClass())) {
      srcValueList = CollectionUtils.convertPrimitiveArrayToList(srcCollectionValue);
    } else {
      srcValueList = Arrays.asList((Object[]) srcCollectionValue);
    }
    return addOrUpdateToList(srcObj, fieldMap, srcValueList, destObj, destEntryType);
  }

  private void writeDestinationValue(Object destObj, Object destFieldValue, FieldMap fieldMap, Object srcObj) {
    boolean bypass = false;
    // don't map null to dest field if map-null="false"
    if (destFieldValue == null && !fieldMap.isDestMapNull()) {
      bypass = true;
    }

    // don't map "" to dest field if map-empty-string="false"
    if (destFieldValue != null && !fieldMap.isDestMapEmptyString() && destFieldValue.getClass().equals(String.class)
        && StringUtils.isEmpty((String) destFieldValue)) {
      bypass = true;
    }

    // trim string value if trim-strings="true"
    if (destFieldValue != null && fieldMap.isTrimStrings() && destFieldValue.getClass().equals(String.class)) {
      destFieldValue = ((String) destFieldValue).trim();
    }

    if (!bypass) {
      eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_PRE_WRITING_DEST_VALUE, fieldMap.getClassMap(), fieldMap, srcObj,
          destObj, destFieldValue));

      fieldMap.writeDestValue(destObj, destFieldValue);

      eventMgr.fireEvent(new DozerEvent(MapperConstants.MAPPING_POST_WRITING_DEST_VALUE, fieldMap.getClassMap(), fieldMap, srcObj,
          destObj, destFieldValue));
    }
  }

  private Object mapUsingCustomConverterInstance(Object converterInstance, Class srcFieldClass, Object srcFieldValue,
      Class destFieldClass, Object existingDestFieldValue, FieldMap fieldMap, boolean topLevel) {
    if (!(converterInstance instanceof CustomConverter)) {
      MappingUtils.throwMappingException("Custom Converter does not implement CustomConverter interface");
    }

    //1792048 - If map-null = "false" and src value is null, then don't even invoke custom converter
    if (srcFieldValue == null && !fieldMap.isDestMapNull()) {
      return null;
    }

    CustomConverter theConverter = (CustomConverter) converterInstance;
    Object result = null;
    long start = System.currentTimeMillis();
    // if this is a top level mapping the destObj is the highest level
    // mapping...not a recursive mapping
    if (topLevel) {
      result = theConverter.convert(existingDestFieldValue, srcFieldValue, destFieldClass, srcFieldClass);
    } else {
      Object existingValue = getExistingValue(fieldMap, existingDestFieldValue, destFieldClass);
      result = theConverter.convert(existingValue, srcFieldValue, destFieldClass, srcFieldClass);
    }
    long stop = System.currentTimeMillis();
    statsMgr.increment(StatisticTypeConstants.CUSTOM_CONVERTER_SUCCESS_COUNT);
    statsMgr.increment(StatisticTypeConstants.CUSTOM_CONVERTER_TIME, stop - start);

    return result;
  }

  // TODO: possibly extract this to a separate class
  private Object mapUsingCustomConverter(Class customConverterClass, Class srcFieldClass, Object srcFieldValue,
      Class destFieldClass, Object existingDestFieldValue, FieldMap fieldMap, boolean topLevel) {
    Object converterInstance = null;
    // search our injected customconverters for a match
    if (customConverterObjects != null) {
      for (int i = 0; i < customConverterObjects.size(); i++) {
        Object customConverter = customConverterObjects.get(i);
        if (customConverter.getClass().isAssignableFrom(customConverterClass)) {
          // we have a match
          converterInstance = customConverter;
        }
      }
    }
    // if converter object instances were not injected, then create new instance
    // of the converter for each conversion
    if (converterInstance == null) {
      converterInstance = ReflectionUtils.newInstance(customConverterClass);
    }
    return mapUsingCustomConverterInstance(converterInstance, srcFieldClass, srcFieldValue, destFieldClass, existingDestFieldValue,
        fieldMap, topLevel);
  }

  private Set checkForSuperTypeMapping(Class srcClass, Class destClass) {
    // Check cache first
    Object cacheKey = CacheKeyFactory.createKey(new Object[] { destClass, srcClass });
    CacheEntry cacheEntry = superTypeCache.get(cacheKey);
    if (cacheEntry != null) {
      return (Set) cacheEntry.getValue();
    }

    // If no existing cache entry is found, determine super type mappings.
    // Recursively walk the inheritance hierarchy.
    Set superClasses = new HashSet();
    // Need to call getRealSuperclass because proxied data objects will not return correct
    // superclass when using basic reflection
    Class superSrcClass = MappingUtils.getRealSuperclass(srcClass);
    Class superDestClass = MappingUtils.getRealSuperclass(destClass);
    boolean stillHasSuperClasses = true;
    while (stillHasSuperClasses) {
      if ((superSrcClass != null && !superSrcClass.getName().equals("java.lang.Object"))
          || (superDestClass != null && !superDestClass.getName().equals("java.lang.Object"))) {
        // see if the source super class is mapped to the dest class
        ClassMap superClassMap = (ClassMap) customMappings.get(ClassMapKeyFactory.createKey(superSrcClass, destClass));
        if (superClassMap != null) {
          superClasses.add(superClassMap);
        }
        // now walk up the dest classes super classes with our super source
        // class and our source class
        boolean stillHasDestSuperClasses = true;
        while (stillHasDestSuperClasses) {
          if (superDestClass != null && !superDestClass.getName().equals("java.lang.Object")) {
            ClassMap superDestClassMap = (ClassMap) customMappings.get(ClassMapKeyFactory.createKey(superSrcClass, superDestClass));
            if (superDestClassMap != null) {
              superClasses.add(superDestClassMap);
            }
            ClassMap srcClassMap = (ClassMap) customMappings.get(ClassMapKeyFactory.createKey(srcClass, superDestClass));
            if (srcClassMap != null) {
              superClasses.add(srcClassMap);
            }
            superDestClass = MappingUtils.getRealSuperclass(superDestClass);
          } else {
            break;
          }
        }
        superSrcClass = MappingUtils.getRealSuperclass(superSrcClass);
      } else {
        break;
      }
    }

    // Add result to cache
    cacheEntry = new CacheEntry(cacheKey, superClasses);
    superTypeCache.put(cacheEntry);

    return superClasses;
  }

  private List processSuperTypeMapping(Set superClasses, Object srcObj, Object destObj, String mapId) {
    List mappedFields = new ArrayList();
    Object[] superClassArray = superClasses.toArray();
    for (int a = 0; a < superClassArray.length; a++) {
      ClassMap map = (ClassMap) superClassArray[a];
      map(map, srcObj, destObj, true, mapId);
      List fieldMaps = map.getFieldMaps();
      for (int i = 0; i < fieldMaps.size(); i++) {
        FieldMap fieldMapping = (FieldMap) fieldMaps.get(i);
        String key = MappingUtils.getMappedParentFieldKey(destObj, fieldMapping.getDestFieldName());
        mappedFields.add(key);
      }
    }
    return mappedFields;
  }

  private static Object getExistingValue(FieldMap fieldMap, Object destObj, Class destFieldType) {
    Object result = null;
    // verify that the dest obj is not null
    if (destObj == null) {
      return null;
    }
    // call the getXX method to see if the field is already instantiated
    result = fieldMap.getDestValue(destObj);

    // When we are recursing through a list we need to make sure that we are not
    // in the list
    // by checking the destFieldType
    if (result != null) {
      if (CollectionUtils.isList(result.getClass()) || CollectionUtils.isArray(result.getClass())
          || CollectionUtils.isSet(result.getClass()) || MappingUtils.isSupportedMap(result.getClass())) {
        if (!CollectionUtils.isList(destFieldType) && !CollectionUtils.isArray(destFieldType)
            && !CollectionUtils.isSet(destFieldType) && !MappingUtils.isSupportedMap(destFieldType)) {
          // this means the getXX field is a List but we are actually trying to
          // map one of its elements
          result = null;
        }
      }
    }
    return result;
  }

  private ClassMap getClassMap(Object srcObj, Class destClass, String mapId) {
    ClassMap mapping = ClassMapFinder.findClassMap(this.customMappings, srcObj.getClass(), destClass, mapId);

    if (mapping == null) {
      // If mapId was specified and mapping was not found, then throw an
      // exception
      if (!MappingUtils.isBlankOrNull(mapId)) {
        MappingUtils.throwMappingException("Class mapping not found for map-id: " + mapId);
      }

      // If mapping not found in existing custom mapping collection, create
      // default as an explicit mapping must not
      // exist. The create default class map method will also add all default
      // mappings that it can determine.
      mapping = ClassMapBuilder.createDefaultClassMap(globalConfiguration, srcObj.getClass(), destClass);
      customMappings.put(ClassMapKeyFactory.createKey(srcObj.getClass(), destClass), mapping);
    }

    return mapping;
  }

}

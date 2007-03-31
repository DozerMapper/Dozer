package net.sf.dozer.util.mapping.util;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.dozer.util.mapping.DozerBeanMapper;
import net.sf.dozer.util.mapping.config.GlobalSettings;
import net.sf.dozer.util.mapping.converters.CustomConverterContainer;
import net.sf.dozer.util.mapping.converters.CustomConverterDescription;
import net.sf.dozer.util.mapping.fieldmap.ClassMap;
import net.sf.dozer.util.mapping.fieldmap.Configuration;
import net.sf.dozer.util.mapping.fieldmap.Mappings;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CustomMappingsLoader {
  
  private static final Log log = LogFactory.getLog(CustomMappingsLoader.class);

  public LoadMappingsResult load(List mappingFiles) {
    Map customMappings = new HashMap();
    ClassMapBuilder classMapBuilder = new ClassMapBuilder();
    ListOrderedSet customConverterDescriptions = new ListOrderedSet();
    Configuration globalConfiguration = null;

    if (mappingFiles != null && mappingFiles.size() > 0) {
      MappingValidator mappingValidator = new MappingValidator();
      
      InitLogger.log(log, "Using the following xml files to load custom mappings for the bean mapper instance: "
          + mappingFiles);
      Iterator iter = mappingFiles.iterator();
      while (iter.hasNext()) {
        String mappingFileName = (String) iter.next();
        InitLogger.log(log, "Trying to find xml mapping file: " + mappingFileName);
        URL url = mappingValidator.validateURL(MapperConstants.DEFAULT_PATH_ROOT + mappingFileName);
        InitLogger.log(log, "Using URL [" + url + "] to load custom xml mappings");
        MappingFileReader mappingFileReader = new MappingFileReader(url);
        Mappings mappings = mappingFileReader.read();
        InitLogger.log(log, "Successfully loaded custom xml mappings from URL: [" + url + "]");

        // the last configuration is the 'global' configuration
        globalConfiguration = mappings.getConfiguration();
        // build up the custom converters to make them global
        if (mappings.getConfiguration() != null && mappings.getConfiguration().getCustomConverters() != null
            && mappings.getConfiguration().getCustomConverters().getConverters() != null) {
          Iterator iterator = mappings.getConfiguration().getCustomConverters().getConverters().iterator();
          while (iterator.hasNext()) {
            CustomConverterDescription cc = (CustomConverterDescription) iterator.next();
            customConverterDescriptions.add(cc);
          }
        }
        MappingsParser mappingsParser = new MappingsParser();
        customMappings.putAll(mappingsParser.parseMappings(mappings));
      }
    }

    // Add default mappings using matching property names if wildcard policy
    // is true. The addDefaultFieldMappings will check the wildcard policy of each classmap
    classMapBuilder.addDefaultFieldMappings(customMappings);
 
    // iterate through the classmaps and set all of the customconverters on them
    Iterator keyIter = customMappings.keySet().iterator();
    while (keyIter.hasNext()) {
      String key = (String) keyIter.next();
      ClassMap classMap = (ClassMap) customMappings.get(key);
      if (classMap.getConfiguration() == null) {
        classMap.setConfiguration(new Configuration());
      }
      if (classMap.getConfiguration().getCustomConverters() != null) {
        classMap.getConfiguration().getCustomConverters().setConverters(customConverterDescriptions.asList());
      } else {
        classMap.getConfiguration().setCustomConverters(new CustomConverterContainer());
        classMap.getConfiguration().getCustomConverters().setConverters(customConverterDescriptions.asList());
      }
    }
    return new LoadMappingsResult(Collections.synchronizedMap(customMappings), globalConfiguration);
  }

}

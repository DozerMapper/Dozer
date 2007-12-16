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
package net.sf.dozer.functional_tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.dozer.util.mapping.DozerBeanMapper;
import net.sf.dozer.util.mapping.MapperIF;
import net.sf.dozer.util.mapping.util.MapperConstants;
import net.sf.dozer.util.mapping.util.TestDataFactory;

/**
 * @author tierney.matt
 * @author garsombke.franz
 */
public abstract class AbstractMapperTest extends TestCase {
  protected MapperIF mapper;
  protected TestDataFactory testDataFactory = new TestDataFactory(getDataObjectInstantiator());

  protected abstract DataObjectInstantiator getDataObjectInstantiator();

  protected void setUp() throws Exception {
    super.setUp();
    System.setProperty("log4j.debug", "true");
    System.setProperty(MapperConstants.DEBUG_SYS_PROP, "true");
    mapper = new DozerBeanMapper();
  }

  protected MapperIF getMapper(String[] mappingFiles) {
    List list = new ArrayList();
    if (mappingFiles != null) {
      for (int i = 0; i < mappingFiles.length; i++) {
        list.add(mappingFiles[i]);
      }
    }
    MapperIF result = new DozerBeanMapper();
    ((DozerBeanMapper) result).setMappingFiles(list);
    return result;
  }

  protected Object newInstance(Class classToInstantiate) {
    return getDataObjectInstantiator().newInstance(classToInstantiate);
  }

}
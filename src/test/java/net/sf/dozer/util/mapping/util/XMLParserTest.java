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

import java.net.URL;

import net.sf.dozer.util.mapping.AbstractDozerTest;
import net.sf.dozer.util.mapping.classmap.Mappings;

/**
 * @author garsombke.franz
 */
public class XMLParserTest extends AbstractDozerTest {

  /*
   * Test method for 'net.sf.dozer.util.mapping.util.XMLParser.parse(InputSource)'
   */
  public void testParse() throws Exception {
    XMLParser parser = new XMLParser();
    ResourceLoader loader = new ResourceLoader();
    URL url = loader.getResource("dozerBeanMapping.xml");

    Mappings mappings = parser.parse(url.openStream());
    assertNotNull(mappings);
  }

}

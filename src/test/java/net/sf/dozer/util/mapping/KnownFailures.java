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

import java.util.ArrayList;
import java.util.List;

import net.sf.dozer.util.mapping.vo.MessageHeaderDTO;
import net.sf.dozer.util.mapping.vo.MessageHeaderVO;
import net.sf.dozer.util.mapping.vo.MessageIdVO;

/**
 * This is a holding grounds for test cases that reproduce known bugs, features, or gaps discovered during development.
 * As the use cases are resolved, these tests should be moved to the live unit test classes.
 * 
 * @author tierney.matt
 */
public class KnownFailures extends AbstractDozerTest {

  /*
   * Feature Request #1731158. Need a way to explicitly specify a mapping between a custom data object and String. Not
   * sure the best way to do this. Copy by reference doesnt seem like a good fit.
   */
  public void testListOfCustomObjectsToStringArray() {
    List mappingFiles = new ArrayList();
    mappingFiles.add("knownFailures.xml");
    MapperIF mapper = new DozerBeanMapper(mappingFiles);

    MessageHeaderVO vo = new MessageHeaderVO();
    List ids = new ArrayList();
    ids.add(new MessageIdVO("1"));
    ids.add(new MessageIdVO("2"));
    vo.setMsgIds(ids);
    MessageHeaderDTO result = null;

    result = (MessageHeaderDTO) mapper.map(vo, MessageHeaderDTO.class);
    assertEquals("1", result.getIdList().getMsgIdsArray()[0]);
    assertEquals("2", result.getIdList().getMsgIdsArray()[1]);
  }

  // Failure discovered during development of an unrelated map type feature request

}
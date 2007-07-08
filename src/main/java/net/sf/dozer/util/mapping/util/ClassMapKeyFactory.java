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

import org.apache.commons.lang.StringUtils;

/**
 * Internal class that generates a unique class mapping key. Only intended for internal use.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 */
public class ClassMapKeyFactory {

  private ClassMapKeyFactory() {
  }

  public static String createKey(Class srcClass, Class destClass, String mapId) {
    StringBuffer result = new StringBuffer(150);
    result.append("SOURCE CLASS-->");
    result.append(srcClass.getName());
    result.append(" DEST CLASS-->");
    result.append(destClass.getName());
    if (StringUtils.isNotEmpty(mapId)) {
      result.append(" MAP ID-->");
      result.append(mapId);
    }
    return result.toString();
  }

  public static String createKey(Class srcClass, Class destClass) {
    return createKey(srcClass, destClass, null);
  }

}

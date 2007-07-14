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
package net.sf.dozer.util.mapping.fieldmap;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.dozer.util.mapping.util.MappingUtils;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Only intended for internal use.
 * 
 * @author garsombke.franz
 * @author sullins.ben
 * @author tierney.matt
 * 
 */
public class HintContainer {
  private String hintName;
  private List hints;

  public Class getHint() {
    Class result = null;
    if (hasMoreThanOneHint()) {
      return null;
    } else {
      result = (Class) getHints().get(0);
    }
    return result;
  }

  public Class getHint(int index) {
    return (Class) getHints().get(index);
  }

  public boolean hasMoreThanOneHint() {
    return getHints().size() > 1 ? true : false;
  }

  public List getHints() {
    if (hints == null) {
      List list = new ArrayList();
      StringTokenizer st = new StringTokenizer(this.hintName, ",");
      while (st.hasMoreElements()) {
        String theHintName = st.nextToken().trim();

        Class clazz = MappingUtils.loadClass(theHintName);
        list.add(clazz);
      }
      hints = list;
    }
    return hints;
  }
  
  //TODO: Refactor/Relocate.  This method doesn't seem to belong in this class
  public Class getHint(Class clazz, List clazzHints) {
    List hints = getHints();
    int hintsSize = hints.size();
    if (hintsSize == 1) {
      return getHint();
    }
    // validate sizes
    if (clazzHints.size() != hintsSize) {
      MappingUtils
          .throwMappingException("When using multiple source and destination hints there must be exactly the same number of hints on the source and the destination.");
    }
    int count = 0;
    String myClazName = clazz.getName();
    int size = clazzHints.size();
    for (int i = 0; i < size; i++) {
      Class element = (Class) clazzHints.get(i);
      if (element.getName().equals(myClazName)) {
        return (Class) hints.get(count++);
      }
      count++;
    }
    return clazz;
  }

  public void setHintName(String hintName) {
    this.hintName = hintName;
  }

  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}

package net.sf.dozer.util.mapping.vo.deepindex.customconverter;

import net.sf.dozer.util.mapping.converters.CustomConverter;

public class Conv implements CustomConverter {

  public Conv() {
  }

  public Object convert(Object destinationObject, Object sourceObject, Class arg2, Class arg3) {
    Third third = new Third();
    third = (Third) sourceObject;
    return third;
  }
}
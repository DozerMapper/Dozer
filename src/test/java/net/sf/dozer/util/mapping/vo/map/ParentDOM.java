package net.sf.dozer.util.mapping.vo.map;

import net.sf.dozer.util.mapping.vo.BaseTestObject;

public class ParentDOM extends BaseTestObject {

  private String test;
  private ChildDOM child;

  public String getTest() {
    return test;
  }
  public void setTest(String test) {
    this.test = test;
  }
  public ChildDOM getChild() {
    return child;
  }
  public void setChild(ChildDOM child) {
    this.child = child;
  }
}

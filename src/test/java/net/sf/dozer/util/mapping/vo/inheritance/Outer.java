package net.sf.dozer.util.mapping.vo.inheritance;

public class Outer {
  Object inner = new Inner();

  public Object getInner() {
    return inner;
  }

  public void setInner(Inner inner) {
    this.inner = inner;
  }
}

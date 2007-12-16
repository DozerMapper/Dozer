package net.sf.dozer.util.mapping.vo.inheritance.iface;

import java.io.Serializable;

public class PersonImpl implements Person, Serializable {
  private Long id;
  private String name;

  protected PersonImpl() {
  }

  public PersonImpl(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

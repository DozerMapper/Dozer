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

import net.sf.dozer.util.mapping.util.TestDataFactory;
import net.sf.dozer.util.mapping.vo.NoSuperClass;
import net.sf.dozer.util.mapping.vo.SubClass;
import net.sf.dozer.util.mapping.vo.SubClassPrime;
import net.sf.dozer.util.mapping.vo.TheFirstSubClass;
import net.sf.dozer.util.mapping.vo.inheritance.A;
import net.sf.dozer.util.mapping.vo.inheritance.AnotherSubClass;
import net.sf.dozer.util.mapping.vo.inheritance.AnotherSubClassPrime;
import net.sf.dozer.util.mapping.vo.inheritance.B;
import net.sf.dozer.util.mapping.vo.inheritance.BaseSubClassCombined;
import net.sf.dozer.util.mapping.vo.inheritance.GenericAbstractSuper;
import net.sf.dozer.util.mapping.vo.inheritance.GenericIF;
import net.sf.dozer.util.mapping.vo.inheritance.S2Class;
import net.sf.dozer.util.mapping.vo.inheritance.S2ClassPrime;
import net.sf.dozer.util.mapping.vo.inheritance.SClass;
import net.sf.dozer.util.mapping.vo.inheritance.SClassPrime;
import net.sf.dozer.util.mapping.vo.inheritance.Specific3;
import net.sf.dozer.util.mapping.vo.inheritance.SpecificObject;
import net.sf.dozer.util.mapping.vo.inheritance.WrapperSpecific;
import net.sf.dozer.util.mapping.vo.inheritance.WrapperSpecificPrime;
import net.sf.dozer.util.mapping.vo.km.Property;
import net.sf.dozer.util.mapping.vo.km.PropertyB;
import net.sf.dozer.util.mapping.vo.km.SomeVo;
import net.sf.dozer.util.mapping.vo.km.Sub;
import net.sf.dozer.util.mapping.vo.km.Super;

import org.apache.commons.lang.SerializationUtils;

/**
 * @author tierney.matt
 * @author garsombke.franz
 */
public class InheritanceMappingTest extends AbstractDozerTest {

  public void testCustomMappingForSuperClasses() throws Exception {
    // Test that the explicit super custom mapping definition is used when mapping sub classes
    mapper = getNewMapper(new String[] { "inheritanceMapping.xml" });

    A src = getA();
    B dest = (B) mapper.map(src, B.class);

    assertNull("superField1 should have been excluded", dest.getSuperField1());
    assertEquals("superBField not mapped correctly", src.getSuperAField(), dest.getSuperBField());
    assertEquals("field1 not mapped correctly", src.getField1(), dest.getField1());
    assertEquals("fieldB not mapped correctly", src.getFieldA(), dest.getFieldB());

    // Remap to each other to test bi-directional mapping
    A mappedSrc = (A) mapper.map(dest, A.class);
    B mappedDest = (B) mapper.map(mappedSrc, B.class);

    assertEquals("objects not mapped correctly bi-directional", dest, mappedDest);
  }

  public void testNoCustomMappingForSuperClasses() throws Exception {
    // Test that wildcard fields in super classes are mapped when there is no explicit super custom mapping definition
    mapper = new DozerBeanMapper();

    A src = getA();
    B dest = (B) mapper.map(src, B.class);

    assertEquals("superField1 not mapped correctly", src.getSuperField1(), dest.getSuperField1());
    assertEquals("field1 not mapped correctly", src.getField1(), dest.getField1());
    assertNull("superBField should not have been mapped", dest.getSuperBField());
    assertNull("fieldB should not have been mapped", dest.getFieldB());

    // Remap to each other to test bi-directional mapping
    A mappedSrc = (A) mapper.map(dest, A.class);
    B mappedDest = (B) mapper.map(mappedSrc, B.class);

    assertEquals("objects not mapped correctly bi-directional", dest, mappedDest);
  }

  public void testNoCustomMappingForSuperClasses_SubclassAttrsAppliedToSuperClasses() throws Exception {
    // Test that when there isnt an explicit super custom mapping definition the subclass mapping def attrs are
    // applied to the super class mapping. In this use case, wildcard="false" for the A --> B mapping definition
    mapper = getNewMapper(new String[] { "inheritanceMapping2.xml" });

    A src = getA();
    B dest = (B) mapper.map(src, B.class);

    assertNull("fieldB should not have been mapped", dest.getSuperField1());
    assertNull("superBField should have not been mapped", dest.getSuperBField());

    // Remap to each other to test bi-directional mapping
    A mappedSrc = (A) mapper.map(dest, A.class);
    B mappedDest = (B) mapper.map(mappedSrc, B.class);

    assertEquals("objects not mapped correctly bi-directional", dest, mappedDest);
  }

  public void testNoCustomMappingForSubclasses_CustomMappingForSuperClasses() throws Exception {
    // Tests that custom mappings for super classes are used when there are no custom mappings
    // for subclasses. Also tests that a default class map is properly created and used for the subclass
    // field mappings
    mapper = getNewMapper(new String[] { "inheritanceMapping3.xml" });

    A src = getA();
    B dest = (B) mapper.map(src, B.class);

    assertNull("superField1 should have been excluded", dest.getSuperField1());
    assertEquals("superBField not mapped correctly", src.getSuperAField(), dest.getSuperBField());
    assertEquals("field1 not mapped correctly", src.getField1(), dest.getField1());

    // Remap to each other to test bi-directional mapping
    A mappedSrc = (A) mapper.map(dest, A.class);
    B mappedDest = (B) mapper.map(mappedSrc, B.class);

    assertEquals("objects not mapped correctly bi-directional", dest, mappedDest);
  }

  public void testGeneralInheritance() throws Exception {
    mapper = getNewMapper(new String[] { "dozerBeanMapping.xml" });
    // first test mapping of sub and base class to a single class
    net.sf.dozer.util.mapping.vo.inheritance.SubClass sub = new net.sf.dozer.util.mapping.vo.inheritance.SubClass();

    sub.setBaseAttribute("base");
    sub.setSubAttribute("sub");

    BaseSubClassCombined combined = (BaseSubClassCombined) mapper.map(sub, BaseSubClassCombined.class);

    assertEquals(sub.getSubAttribute(), combined.getSubAttribute2());
    assertEquals(sub.getBaseAttribute(), combined.getBaseAttribute2());
  }

  public void testGeneralInheritance2() throws Exception {
    mapper = getNewMapper(new String[] { "dozerBeanMapping.xml" });
    // test base to base and sub to sub mapping with an intermediate on the destination
    AnotherSubClass asub = new AnotherSubClass();
    asub.setBaseAttribute("base");
    asub.setSubAttribute("sub");
    List list = new ArrayList();
    SClass s = new SClass();
    s.setBaseSubAttribute("sBase");
    s.setSubAttribute("s");
    S2Class s2 = new S2Class();
    s2.setBaseSubAttribute("s2Base");
    s2.setSub2Attribute("s2");
    list.add(s2);
    list.add(s);
    asub.setSubList(list);

    List list2 = new ArrayList();
    SClass sclass = new SClass();
    sclass.setBaseSubAttribute("sBase");
    sclass.setSubAttribute("s");
    S2Class s2class = new S2Class();
    s2class.setBaseSubAttribute("s2Base");
    s2class.setSub2Attribute("s2");
    SClass sclass2 = new SClass();
    sclass2.setBaseSubAttribute("sclass2");
    sclass2.setSubAttribute("sclass2");
    list2.add(s2class);
    list2.add(sclass);
    list2.add(sclass2);
    asub.setListToArray(list2);

    SClass sclassA = new SClass();
    SClass sclassB = new SClass();
    sclassA.setBaseSubAttribute("sBase");
    sclassA.setSubAttribute("s");
    sclassB.setBaseSubAttribute("sBase");
    sclassB.setSubAttribute("s");
    asub.setSClass(sclassA);
    asub.setSClass2(sclassB);

    AnotherSubClassPrime subPrime = (AnotherSubClassPrime) mapper.map(asub, AnotherSubClassPrime.class);

    assertEquals(asub.getSubAttribute(), subPrime.getSubAttribute2());
    assertEquals(asub.getBaseAttribute(), subPrime.getBaseAttribute2());
    assertTrue(subPrime.getTheListOfSubClassPrime().get(0) instanceof S2ClassPrime);
    assertTrue(subPrime.getTheListOfSubClassPrime().get(1) instanceof SClassPrime);
    assertEquals(s2.getSub2Attribute(), ((S2ClassPrime) subPrime.getTheListOfSubClassPrime().get(0)).getSub2Attribute2());
    assertEquals(s2.getBaseSubAttribute(), ((S2ClassPrime) subPrime.getTheListOfSubClassPrime().get(0)).getBaseSubAttribute2());
    assertEquals(s.getSubAttribute(), ((SClassPrime) subPrime.getTheListOfSubClassPrime().get(1)).getSubAttribute2());
    assertEquals(s.getBaseSubAttribute(), ((SClassPrime) subPrime.getTheListOfSubClassPrime().get(1)).getBaseSubAttribute2());
    assertTrue(subPrime.getArrayToList()[0] instanceof S2ClassPrime);
    assertTrue(subPrime.getArrayToList()[1] instanceof SClassPrime);
    assertTrue(subPrime.getArrayToList()[2] instanceof SClassPrime);
    assertEquals(s2class.getSub2Attribute(), ((S2ClassPrime) subPrime.getArrayToList()[0]).getSub2Attribute2());
    assertEquals(s2class.getBaseSubAttribute(), ((S2ClassPrime) subPrime.getArrayToList()[0]).getBaseSubAttribute2());
    assertEquals(sclass.getSubAttribute(), ((SClassPrime) subPrime.getArrayToList()[1]).getSubAttribute2());
    assertEquals(sclass.getBaseSubAttribute(), ((SClassPrime) subPrime.getArrayToList()[1]).getBaseSubAttribute2());
    assertEquals(sclass2.getSubAttribute(), ((SClassPrime) subPrime.getArrayToList()[2]).getSubAttribute2());
    assertEquals(sclass2.getBaseSubAttribute(), ((SClassPrime) subPrime.getArrayToList()[2]).getBaseSubAttribute2());
    assertEquals(asub.getSClass().getSubAttribute(), subPrime.getSClass().getSubAttribute2());
    assertEquals(asub.getSClass().getBaseSubAttribute(), subPrime.getSClass().getBaseSubAttribute2());
    assertEquals(asub.getSClass2().getSubAttribute(), subPrime.getSClass2().getSubAttribute2());
    assertEquals(asub.getSClass2().getBaseSubAttribute(), subPrime.getSClass2().getBaseSubAttribute2());

    // map it back
    AnotherSubClass sub = (AnotherSubClass) mapper.map(subPrime, AnotherSubClass.class);
    assertTrue(sub.getSubList().get(0) instanceof S2Class);
    assertTrue(sub.getSubList().get(1) instanceof SClass);
    assertEquals(s2.getSub2Attribute(), ((S2Class) sub.getSubList().get(0)).getSub2Attribute());
    assertEquals(s2.getBaseSubAttribute(), ((S2Class) sub.getSubList().get(0)).getBaseSubAttribute());
    assertEquals(s.getSubAttribute(), ((SClass) sub.getSubList().get(1)).getSubAttribute());
    assertEquals(s.getBaseSubAttribute(), ((SClass) sub.getSubList().get(1)).getBaseSubAttribute());
  }

  public void testInheritanceWithAbstractClassOrInterfaceAsDestination() throws Exception {
    mapper = getNewMapper(new String[] { "dozerBeanMapping.xml" });
    SpecificObject so = new SpecificObject();
    so.setSuperAttr1("superAttr1");

    // validate abstract class
    Object obj = mapper.map(so, GenericAbstractSuper.class);
    assertTrue(obj instanceof Specific3);
    Specific3 spec3 = (Specific3) obj;
    assertEquals("superAttr1", spec3.getSuperAttr3());
    assertEquals("superAttr1", spec3.getSuperAttr2());

    // validate interface
    obj = mapper.map(so, GenericIF.class);
    assertTrue(obj instanceof Specific3);
    spec3 = (Specific3) obj;
    assertEquals("superAttr1", spec3.getSuperAttr3());
    assertEquals("superAttr1", spec3.getSuperAttr2());

    WrapperSpecific ws = new WrapperSpecific();
    SpecificObject so2 = new SpecificObject();
    so2.setSuperAttr1("superAttr1");
    ws.setSpecificObject(so2);
    WrapperSpecificPrime wsp = (WrapperSpecificPrime) mapper.map(ws, WrapperSpecificPrime.class);
    assertTrue(wsp.getSpecificObjectPrime() instanceof Specific3);
    assertEquals("superAttr1", ((Specific3) wsp.getSpecificObjectPrime()).getSuperAttr3());
    assertEquals("superAttr1", ((Specific3) wsp.getSpecificObjectPrime()).getSuperAttr2());
  }

  public void testComplexSuperClassMapping() throws Exception {
    mapper = getNewMapper(new String[] { "dozerBeanMapping.xml" });
    SubClass obj = TestDataFactory.getSubClass();
    SubClassPrime objPrime = (SubClassPrime) mapper.map(obj, SubClassPrime.class);
    SubClass obj2 = (SubClass) mapper.map(objPrime, SubClass.class);
    SubClassPrime objPrime2 = (SubClassPrime) mapper.map(obj2, SubClassPrime.class);

    assertEquals("" + obj.getCustomConvert().getAttribute().getTheDouble(), obj2.getCustomConvert().getAttribute().getTheDouble()
        + "");

    // one-way mapping
    objPrime.setSuperFieldToExcludePrime(null);
    assertEquals(objPrime, objPrime2);

    // Pass by reference
    obj = TestDataFactory.getSubClass();
    SubClass subClassClone = (SubClass) SerializationUtils.clone(obj);
    objPrime = (SubClassPrime) mapper.map(obj, SubClassPrime.class);
    mapper.map(objPrime, obj);
    obj.setCustomConvert(null);
    subClassClone.setCustomConvert(null);
    // more objects should be added to the clone from the ArrayList
    TheFirstSubClass fsc = new TheFirstSubClass();
    fsc.setS("s");
    subClassClone.getTestObject().getHintList().add(fsc);
    subClassClone.getTestObject().getHintList().add(fsc);
    subClassClone.getTestObject().getEqualNamedList().add("1value");
    subClassClone.getTestObject().getEqualNamedList().add("2value");
    int[] pa = { 0, 1, 2, 3, 4, 0, 1, 2, 3, 4 };
    int[] intArray = { 1, 1, 1, 1 };
    Integer[] integerArray = { new Integer(1), new Integer(1), new Integer(1), new Integer(1) };
    subClassClone.getTestObject().setAnArray(intArray);
    subClassClone.getTestObject().setArrayForLists(integerArray);
    subClassClone.getTestObject().setPrimArray(pa);
    subClassClone.getTestObject().setBlankDate(null);
    subClassClone.getTestObject().setBlankStringToLong(null);
    subClassClone.getSuperList().add("one");
    subClassClone.getSuperList().add("two");
    subClassClone.getSuperList().add("three");
    // since we copy by reference the attribute copyByReference we need to null it out. The clone method above creates
    // two versions of it...
    // which is incorrect
    obj.getTestObject().setCopyByReference(null);
    subClassClone.getTestObject().setCopyByReference(null);
    obj.getTestObject().setCopyByReferenceDeep(null);
    subClassClone.getTestObject().setCopyByReferenceDeep(null);
    obj.getTestObject().setGlobalCopyByReference(null);
    subClassClone.getTestObject().setGlobalCopyByReference(null);
    // null out string array because we get NPE since a NULL value in the String []
    obj.getTestObject().setStringArrayWithNullValue(null);
    subClassClone.getTestObject().setStringArrayWithNullValue(null);
    subClassClone.getTestObject().setExcludeMeOneWay("excludeMeOneWay");
    assertEquals(subClassClone, obj);
  }

  public void testSuperClassMapping() throws Exception {
    // source object does not extend a base custom data object, but destination object extends a custom data object.
    mapper = getNewMapper(new String[] { "dozerBeanMapping.xml" });
    NoSuperClass src = new NoSuperClass();
    src.setAttribute("somefieldvalue");
    src.setSuperAttribute("someotherfieldvalue");

    SubClass dest = (SubClass) mapper.map(src, SubClass.class);
    NoSuperClass src1 = (NoSuperClass) mapper.map(dest, NoSuperClass.class);
    SubClass dest2 = (SubClass) mapper.map(src1, SubClass.class);

    assertEquals(src, src1);
    assertEquals(dest, dest2);
  }

  /*
   * Related to bug #1486105
   */
  public void testKM1() {
    SomeVo request = new SomeVo();
    request.setUserName("yo");
    request.setAge("2");
    request.setColor("blue");

    MapperIF mapper = getNewMapper(new String[] { "kmmapping.xml" });

    Super afterMapping = (Super) mapper.map(request, Super.class);

    assertNotNull("login name should not be null", afterMapping.getLoginName());
    assertNotNull("age should not be null", afterMapping.getAge());
    assertEquals("should map SuperClass.name to SubClassPrime userName.", request.getUserName(), afterMapping.getLoginName());
    assertEquals(request.getAge(), afterMapping.getAge());
  }

  /*
   * Bug #1486105 and #1757573
   */
  public void testKM2() {
    Sub request = new Sub();
    request.setAge("2");
    request.setColor("blue");
    request.setLoginName("fred");
    Property property = new Property();
    PropertyB nestedProperty = new PropertyB();
    nestedProperty.setTestProperty("boo");
    property.setTestProperty("testProperty");
    property.setMapMe(nestedProperty);
    request.setProperty(property);

    MapperIF mapper = getNewMapper(new String[] { "kmmapping.xml" });

    SomeVo afterMapping = (SomeVo) mapper.map(request, SomeVo.class);

    assertNotNull("un should not be null", afterMapping.getUserName());
    assertNotNull("color should not be null", afterMapping.getColor());
    assertNotNull("age should not be null", afterMapping.getAge());
    assertEquals("should map SuperClass.name to SubClassPrime userName.", request.getLoginName(), afterMapping.getUserName());
    assertEquals(request.getColor(), afterMapping.getColor());
    assertEquals(request.getAge(), afterMapping.getAge());
    assertEquals(request.getProperty().getMapMe().getTestProperty(), afterMapping.getProperty().getTestProperty());
    assertNull(afterMapping.getProperty().getMapMe());
  }

  private A getA() {
    A result = new A();
    result.setField1("field1value");
    result.setFieldA("fieldAValue");
    result.setSuperAField("superAFieldValue");
    result.setSuperField1("superField1Value");
    return result;
  }
}
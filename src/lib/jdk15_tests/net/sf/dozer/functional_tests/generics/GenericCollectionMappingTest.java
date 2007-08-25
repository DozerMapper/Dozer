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
package net.sf.dozer.functional_tests.generics;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.dozer.functional_tests.AbstractMapperTest;
import net.sf.dozer.functional_tests.DataObjectInstantiator;
import net.sf.dozer.functional_tests.NoProxyDataObjectInstantiator;
import net.sf.dozer.util.mapping.MapperIF;
import net.sf.dozer.util.mapping.vo.generics.deepindex.AnotherTestObject;
import net.sf.dozer.util.mapping.vo.generics.deepindex.DestDeepObj;
import net.sf.dozer.util.mapping.vo.generics.deepindex.Family;
import net.sf.dozer.util.mapping.vo.generics.deepindex.HeadOfHouseHold;
import net.sf.dozer.util.mapping.vo.generics.deepindex.PersonalDetails;
import net.sf.dozer.util.mapping.vo.generics.deepindex.Pet;
import net.sf.dozer.util.mapping.vo.generics.deepindex.SrcDeepObj;
import net.sf.dozer.util.mapping.vo.generics.deepindex.TestObject;
import net.sf.dozer.util.mapping.vo.generics.deepindex.TestObjectPrime;

/**
 * @author garsombke.franz
 */
public class GenericCollectionMappingTest extends AbstractMapperTest {

  public void testGenericCollectionMapping() throws Exception {
    MapperIF mapper = getMapper(new String[] { "genericCollectionMapping.xml" });

    // prepare beans
    User user1 = new User();
    user1.setFirstName("first name 1");
    user1.setLastName("last name 1");

    User user2 = new User();
    user2.setFirstName("first name 2");
    user2.setLastName("last name 2");

    Set<User> users = new HashSet<User>();
    users.add(user1);
    users.add(user2);

    UserGroup userGroup = new UserGroup();
    userGroup.setName("usergroup name");
    userGroup.setUsers(users);
    userGroup.setStatus(Status.SUCCESS);

    // do mapping
    UserGroupPrime userGroupPrime = (UserGroupPrime) mapper.map(userGroup, UserGroupPrime.class);

    // prove access to generic type information
    Method setter = UserGroupPrime.class.getMethod("setUsers", List.class);
    Type[] parameterTypes = setter.getGenericParameterTypes();
    assertEquals(1, parameterTypes.length);
    ParameterizedType parameterType = (ParameterizedType) parameterTypes[0];
    assertEquals(List.class, parameterType.getRawType());
    assertEquals(UserPrime.class, parameterType.getActualTypeArguments()[0]);

    // check group
    assertNotNull(userGroupPrime);
    assertEquals(userGroup.getName(), userGroupPrime.getName());

    // check resulting collection
    List<?> usersPrime = userGroupPrime.getUsers();
    assertNotNull(usersPrime);
    assertEquals(2, usersPrime.size());
    assertTrue("Expecting instance of UserPrime.", usersPrime.get(0) instanceof UserPrime);
    assertTrue("Expecting instance of UserPrime.", usersPrime.get(1) instanceof UserPrime);
    assertEquals("SUCCESS", userGroupPrime.getStatusPrime().name());
    
    // Map the other way
    UserGroup userGroupMapBack = (UserGroup) mapper.map(userGroupPrime, UserGroup.class);
    Set<?> usersGroupPrime = userGroupMapBack.getUsers();
    assertNotNull(usersGroupPrime);
    assertEquals(2, usersGroupPrime.size());
    assertTrue("Expecting instance of UserPrime.", usersGroupPrime.iterator().next() instanceof User);
  }
  
  public void testDeepMappingWithIndexOnSrcField() {
    MapperIF mapper = getMapper(new String[] { "genericCollectionMapping.xml" });
    
    AnotherTestObject anotherTestObject = new AnotherTestObject();
    anotherTestObject.setField3("another test object field 3 value");
    anotherTestObject.setField4("6453");

    TestObject testObject1 = new TestObject();
    TestObject testObject2 = new TestObject();
    testObject2.setEqualNamedList(Arrays.asList(new AnotherTestObject[]{anotherTestObject}));
    
    SrcDeepObj src = new SrcDeepObj();
    src.setSomeList(Arrays.asList(new TestObject[] {testObject1, testObject2}));

    DestDeepObj dest = (DestDeepObj) mapper.map(src, DestDeepObj.class);
    assertEquals("another test object field 3 value", dest.getDest5());
    assertEquals(Integer.valueOf("6453"), ((TestObjectPrime)dest.getHintList().get(0)).getTwoPrime());
  }

  public void testDeepMappingWithIndexOnDestField() {
    MapperIF mapper = getMapper(new String[] { "genericCollectionMapping.xml" });
    DestDeepObj src = new DestDeepObj();
    src.setDest5("some string value for field");

    SrcDeepObj dest = (SrcDeepObj) mapper.map(src, SrcDeepObj.class);
    assertEquals("some string value for field", dest.getSomeList().get(1).getEqualNamedList().get(0).getField3());
  }

  public void testDeepMapIndexed() throws Exception {
    MapperIF mapper = getMapper(new String[] { "genericCollectionMapping.xml" });
    Pet[] myPets = new Pet[2];
    Family source = new Family("john", "jane", "doe", new Integer(22000), new Integer(20000));
    Pet firstPet = new Pet("molly", 2, null);
    myPets[0] = firstPet;

    Pet[] offSprings = new Pet[4];
    offSprings[0] = new Pet("Rocky1", 1, null);
    offSprings[1] = new Pet("Rocky2", 1, null);
    offSprings[2] = new Pet("Rocky3", 1, null);
    offSprings[3] = new Pet("Rocky4", 1, null);

    Pet secondPet = new Pet("Rocky", 3, offSprings);
    myPets[1] = secondPet;

    // Save the pet details into the source object
    source.setPets(myPets);

    HeadOfHouseHold dest = (HeadOfHouseHold) mapper.map(source, HeadOfHouseHold.class);
    assertEquals(((PersonalDetails) source.getFamilyMembers().get(0)).getFirstName(), dest.getFirstName());
    assertEquals(((PersonalDetails) source.getFamilyMembers().get(0)).getLastName(), dest.getLastName());
    assertEquals(((PersonalDetails) source.getFamilyMembers().get(0)).getSalary(), dest.getSalary());
    assertEquals(source.getPets()[1].getPetName(), dest.getPetName());
    assertEquals(String.valueOf(source.getPets()[1].getPetAge()), dest.getPetAge());
    assertEquals(source.getPets()[1].getOffSpring()[2].getPetName(), dest.getOffSpringName());
  }

  public void testDeepMapInvIndexed() throws Exception {
    MapperIF mapper = getMapper(new String[] { "genericCollectionMapping.xml" });
    HeadOfHouseHold source = new HeadOfHouseHold();
    source.setFirstName("Tom");
    source.setLastName("Roy");
    source.setPetName("Ronny");
    source.setSalary(new Integer(15000));
    source.setPetAge("2");
    source.setOffSpringName("Ronny2");

    Family dest = new Family();
    mapper.map(source, dest);

    assertEquals(((PersonalDetails) dest.getFamilyMembers().get(0)).getFirstName(), source.getFirstName());
    assertEquals(((PersonalDetails) dest.getFamilyMembers().get(0)).getLastName(), source.getLastName());
    assertEquals(((PersonalDetails) dest.getFamilyMembers().get(0)).getSalary(), source.getSalary());
    assertEquals(dest.getPets()[1].getPetName(), source.getPetName());
    assertEquals(String.valueOf(dest.getPets()[1].getPetAge()), source.getPetAge());
    assertEquals(dest.getPets()[1].getOffSpring()[2].getPetName(), source.getOffSpringName());
  }
  
  protected DataObjectInstantiator getDataObjectInstantiator() {
    return NoProxyDataObjectInstantiator.INSTANCE;
  }
  
}

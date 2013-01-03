package org.dozer;

import org.dozer.vo.A;
import org.dozer.vo.B;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Spikhalskiy
 * @since 03.01.13
 */
public class MappingProcessorTest extends AbstractDozerTest {
  private ArrayList<Object> sourceList;
  private ArrayList<Object> destinationList;

  @Before
  public void setUp() {
    sourceList = new ArrayList<Object>();
    destinationList = new ArrayList<Object>();
  }

  @Test
  public void testTwiceObjectToObjectConvert() {
    DozerBeanMapper mapper = new DozerBeanMapper();
    Mapper mappingProcessor = mapper.getMappingProcessor();

    A src = new A();
    src.setB(new B());

    A dest1 = new A();
    mappingProcessor.map(src, dest1);
    A dest2 = new A();
    mappingProcessor.map(src, dest2);

    assertSame(dest1.getB(), dest2.getB());
  }

  @Test
  public void testPrepareDetinationList_OK() {
    List<?> result = MappingProcessor.prepareDestinationList(sourceList, destinationList);
    assertEquals(destinationList, result);

    destinationList.add("");
    result = MappingProcessor.prepareDestinationList(sourceList, destinationList);
    assertEquals(destinationList, result);
  }

  @Test
  public void testPrepareDetinationList_Null() {
    List<?> result = MappingProcessor.prepareDestinationList(sourceList, null);
    assertNotNull(result);
    assertEquals(new ArrayList<Object>(), result);
  }

  @Test
  public void testPrepareDetinationList_Array() {
    List<?> result = MappingProcessor.prepareDestinationList(sourceList, new Object[] { "A" });
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("A", result.iterator().next());
  }

  @Test
  public void testPrepareDetinationList_StrangeCase() {
    List<?> result = MappingProcessor.prepareDestinationList(sourceList, "Hullo");
    assertNotNull(result);
    assertEquals(new ArrayList<Object>(), result);
  }

  @Test
  public void testRemoveOrphans_OK() {
    destinationList.add("A");
    MappingProcessor.removeOrphans(sourceList, destinationList);
    assertTrue(destinationList.isEmpty());
  }

  @Test
  public void testRemoveOrphans_Many() {
    destinationList.add("A");
    destinationList.add("B");
    destinationList.add("C");

    sourceList.add("B");
    sourceList.add("D");

    MappingProcessor.removeOrphans(sourceList, destinationList);
    assertEquals(2, destinationList.size());
    assertEquals("B", destinationList.get(0));
    assertEquals("D", destinationList.get(1));
  }

  @Test
  public void testRemoveOrphans_Ordering() {
    destinationList.add(new Ordered(1));
    destinationList.add(new Ordered(2));
    destinationList.add(new Ordered(3));

    sourceList.add(new Ordered(0));
    sourceList.add(new Ordered(3));
    sourceList.add(new Ordered(2));
    sourceList.add(new Ordered(1));

    MappingProcessor.removeOrphans(sourceList, destinationList);
    assertEquals(4, destinationList.size());
    assertEquals(new Ordered(1), destinationList.get(0));
    assertEquals(new Ordered(2), destinationList.get(1));
    assertEquals(new Ordered(3), destinationList.get(2));
    assertEquals(new Ordered(0), destinationList.get(3));
  }

  private static class Ordered {
    private int id;

    private Ordered(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Ordered ordered = (Ordered) o;
      if (id != ordered.id)
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      return id;
    }
  }
}

package net.zerobuilder.examples.beans.more;

import org.junit.Test;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystBuilder;
import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BusinessAnalystTest {

  @Test
  public void testCollectionOneToTwo() throws Exception {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .executive(false)
        .name("Peter")
        .notes(asList("entry"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .executive(true)
        .age(37)
        .notes(asList("entry0", "entry1"))
        .done();
    assertThat(peter.getAge(), is(36));
    assertThat(peter.getName(), is("Peter"));
    assertThat(peter.getNotes(), is(singletonList("entry")));
    assertThat(peter.isExecutive(), is(false));
    assertThat(updated.getAge(), is(37));
    assertThat(updated.getName(), is("Peter"));
    assertThat(updated.getNotes(), is(asList("entry0", "entry1")));
    assertThat(updated.isExecutive(), is(true));
  }

  @Test
  public void testCollectionTwoToOne() throws Exception {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .executive(true)
        .name("Peter")
        .notes(asList("entry0", "entry1"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .age(37)
        .executive(false)
        .notes(singletonList("entry"))
        .done();
    assertThat(peter.getAge(), is(36));
    assertThat(peter.getName(), is("Peter"));
    assertThat(peter.getNotes(), is(asList("entry0", "entry1")));
    assertThat(peter.isExecutive(), is(true));
    assertThat(updated.getAge(), is(37));
    assertThat(updated.getName(), is("Peter"));
    assertThat(updated.getNotes(), is(singletonList("entry")));
    assertThat(updated.isExecutive(), is(false));
  }
}
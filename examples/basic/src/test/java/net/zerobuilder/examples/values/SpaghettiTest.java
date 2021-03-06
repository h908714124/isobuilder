package net.zerobuilder.examples.values;

import org.junit.Test;

import static net.zerobuilder.examples.values.SpaghettiBuilders.spaghettiUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class SpaghettiTest {

  @Test
  public void napoliBuilder() throws Exception {
    Spaghetti spaghetti = Spaghetti.napoliBuilder()
        .cheese("reggiano")
        .alDente(true);
    assertTrue(spaghetti.alDente);
    assertThat(spaghetti.cheese, is("reggiano"));
    assertThat(spaghetti.sauce, is("tomato"));
    spaghetti = spaghettiUpdater(spaghetti)
        .sauce("hot salsa")
        .cheese("cheddar")
        .alDente(false).done();
    assertFalse(spaghetti.alDente);
    assertThat(spaghetti.cheese, is("cheddar"));
    assertThat(spaghetti.sauce, is("hot salsa"));
  }

}
package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.updater.RegularUpdater;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.Access.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RegularUpdaterTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);

  // "goal type", see below
  private static final ClassName TYPE = ClassName.get(RegularUpdaterTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(RegularUpdaterTest.class)
      .nestedClass("MyTypeBuilders");
  public static final RegularUpdater REGULAR_UPDATER_MODULE = new RegularUpdater();

  /**
   * <p>We want to generate updater for the following
   * </p>
   * <pre><code>
   *   class MyType {
   *     MyType(String foo) {
   *     }
   *     String getFoo() throws IOException {
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void test() {

    DtoContext.GoalContext goalContext = createContext(TYPE, GENERATED_TYPE);

    String goalName = "myGoal";
    ConstructorGoalDetails details = ConstructorGoalDetails.create(
        TYPE, goalName, singletonList("foo"),
        PUBLIC,
        emptyList(),
        NEW_INSTANCE);

    // use ProjectedParameter because the updater module requires projections
    ProjectedParameter fooParameter = DtoRegularParameter.create("foo", STRING,
        ProjectionMethod.create("getFoo", singletonList(IO_EXCEPTION)));
    ProjectedRegularGoalDescription description = ProjectedRegularGoalDescription.create(
        details,
        Collections.emptyList(),
        singletonList(fooParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        singletonList(new DtoGeneratorInput.ProjectedGoalInput(REGULAR_UPDATER_MODULE, description)));

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("myGoalUpdater"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(1));
    assertThat(generatorOutput.methods().get(0).method().exceptions, is(singletonList(IO_EXCEPTION)));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorOutput.methods().get(0).method().returnType,
        is(GENERATED_TYPE.nestedClass("MyGoalUpdater")));

    // Get the definition of the generated type
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertThat(typeSpec.name, is("MyTypeBuilders"));
    assertThat(typeSpec.methodSpecs.size(), is(2)); // myGoalToBuilder, constructor
  }
}
package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.RegularGoalDescription;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class DtoGoalDescription {

  public static abstract class GoalDescription {
    public final AbstractGoalDetails details() {
      return goalDetails.apply(this);
    }
    public final String name() {
      return details().name;
    }
    public final List<TypeName> thrownTypes() {
      return thrownTypes.apply(this);
    }
    public abstract <R> R accept(GoalDescriptionCases<R> cases);
  }

  interface GoalDescriptionCases<R> {
    R regularGoal(RegularGoalDescription goal);
    R beanGoal(BeanGoalDescription goal);
  }

  static <R> Function<GoalDescription, R> asFunction(final GoalDescriptionCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<GoalDescription, R> goalDescriptionCases(
      Function<? super RegularGoalDescription, ? extends R> regularGoal,
      Function<? super BeanGoalDescription, ? extends R> beanGoal) {
    return asFunction(new GoalDescriptionCases<R>() {
      @Override
      public R regularGoal(RegularGoalDescription goal) {
        return regularGoal.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalDescription goal) {
        return beanGoal.apply(goal);
      }
    });
  }

  private static final Function<GoalDescription, AbstractGoalDetails> goalDetails =
      goalDescriptionCases(
          description -> description.details,
          description -> description.details);

  private static final Function<GoalDescription, List<TypeName>> thrownTypes =
      goalDescriptionCases(
          regularGoal -> regularGoal.thrownTypes,
          beanGoal -> Collections.emptyList());

  private static String goalName(GoalDescription goal) {
    return goalDetails.apply(goal).name;
  }

  private DtoGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}

package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

final class DtoGoalContext {

  static abstract class AbstractGoalContext {

    abstract <R> R accept(GoalCases<R> cases);

    final List<? extends AbstractStep> steps() {
      return abstractSteps.apply(this);
    }

    final String name() {
      return goalName.apply(this);
    }

    final AbstractGoalDetails details() {
      return abstractGoalDetails.apply(this);
    }

    final BuildersContext context() {
      return context.apply(this);
    }

    final TypeName goalType() {
      return goalType.apply(this);
    }
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R> goalCases(
      Function<? super RegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
  }

  static final Function<RegularGoalContext, BuildersContext> regularContext =
      DtoRegularGoalContext.regularGoalContextCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.context,
              method -> method.context),
          DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
              method -> method.context,
              constructor -> constructor.context));

  static final Function<AbstractGoalContext, BuildersContext> context =
      goalCases(
          regularContext,
          bean -> bean.context);

  private static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          DtoRegularGoalContext.regularGoalContextCases(
              DtoRegularGoal.goalDetails,
              DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
                  method -> method.details,
                  constructor -> constructor.details)),
          bean -> bean.details);

  private static final Function<AbstractGoalContext, TypeName> goalType =
      goalCases(
          DtoRegularGoalContext.regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.goalType,
                  method -> method.details.goalType),
              DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
                  method -> method.details.goalType,
                  constructor -> constructor.details.goalType)),
          bean -> bean.details.goalType);

  private static final Function<AbstractGoalContext, String> goalName =
      goalCases(
          DtoRegularGoalContext.regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.name,
                  method -> method.details.name),
              DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
                  method -> method.details.name,
                  constructor -> constructor.details.name)),
          bean -> bean.details.name);

  static final Function<AbstractGoalContext, List<? extends AbstractStep>> abstractSteps =
      goalCases(
          DtoRegularGoalContext.regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.steps,
                  method -> method.steps),
              DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
                  method -> method.steps,
                  constructor -> constructor.steps)),
          bean -> unmodifiableList(bean.steps));

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}

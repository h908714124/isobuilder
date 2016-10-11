package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;

import java.util.List;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;

final class DtoBeanGoalContext {

  static final class BeanGoal implements IGoal {

    final List<? extends AbstractBeanStep> steps;
    final BeanGoalDetails details;

    private BeanGoal(BeanGoalDetails details,
                     List<? extends AbstractBeanStep> steps) {
      this.steps = steps;
      this.details = details;
    }

    static BeanGoal create(BeanGoalDetails details,
                           List<? extends AbstractBeanStep> steps) {
      return new BeanGoal(details, steps);
    }

    /**
     * A mutable field that holds an instance of the bean type.
     *
     * @return field spec
     */
    FieldSpec bean() {
      return fieldSpec(details.goalType,
          downcase(details.goalType.simpleName()), PRIVATE);
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new BeanGoalContext(this, context);
    }
  }

  static final class BeanGoalContext implements AbstractGoalContext {

    final BuildersContext builders;
    final BeanGoal goal;

    BeanGoalContext(BeanGoal goal,
                    BuildersContext builders) {
      this.goal = goal;
      this.builders = builders;
    }

    public <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoBeanGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}

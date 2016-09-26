package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal.AbstractGoal;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidRegularParameter;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidBeanGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidRegularGoal;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;

public final class GoalContextFactory {

  static AbstractGoalContext context(final DtoValidGoal.ValidGoal validGoal, final BuildersContext builders,
                                     final boolean toBuilder, final boolean builder) throws ValidationException {
    return validGoal.accept(new DtoValidGoal.ValidGoalCases<AbstractGoalContext>() {
      @Override
      public AbstractGoalContext regularGoal(ValidRegularGoal goal) {
        ClassName contractName = contractName(goal.goal.goal, builders);
        ImmutableList<TypeName> thrownTypes = thrownTypes(goal.goal.executableElement);
        ImmutableList<RegularStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            thrownTypes,
            regularParameterFactory);
        return new RegularGoalContext(
            goal.goal.goal, builders, toBuilder, builder, contractName, steps, thrownTypes);
      }
      @Override
      public AbstractGoalContext beanGoal(ValidBeanGoal goal) {
        ClassName contractName = contractName(goal.goal.goal, builders);
        ImmutableList<BeanStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            ImmutableList.<TypeName>of(),
            beansParameterFactory);
        FieldSpec field = FieldSpec.builder(goal.goal.goal.goalType,
            downcase(goal.goal.goal.goalType.simpleName()), PRIVATE).build();
        return new BeanGoalContext(
            goal.goal.goal, builders, toBuilder, builder, contractName, steps, field);
      }
    });
  }

  private static <P extends ValidParameter, R extends AbstractStep>
  ImmutableList<R> steps(ClassName builderType,
                         TypeName nextType,
                         ImmutableList<P> parameters,
                         ImmutableList<TypeName> thrownTypes,
                         ParameterFactory<P, R> parameterFactory) {
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (P parameter : parameters.reverse()) {
      ClassName thisType = builderType.nestedClass(upcase(parameter.name));
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends ValidParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<ValidBeanParameter, BeanStep> beansParameterFactory
      = new ParameterFactory<ValidBeanParameter, BeanStep>() {
    @Override
    BeanStep create(ClassName thisType, TypeName nextType, ValidBeanParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      ParameterSpec parameter = parameterSpec(validParameter.type, validParameter.name);
      String setter = validParameter.collectionType.isPresent() ? "" : "set" + upcase(validParameter.name);
      return new BeanStep(thisType, nextType, validParameter, parameter, setter);
    }
  };

  private static final ParameterFactory<ValidRegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<ValidRegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName thisType, TypeName nextType, ValidRegularParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      FieldSpec field = FieldSpec.builder(validParameter.type, validParameter.name, PRIVATE).build();
      ParameterSpec parameter = parameterSpec(validParameter.type, validParameter.name);
      return new RegularStep(thisType, nextType, validParameter, declaredExceptions, field, parameter);
    }
  };

  private static ClassName contractName(AbstractGoal goal, BuildersContext config) {
    return config.generatedType.nestedClass(upcase(goal.name + "Builder"));
  }

  public enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement executableElement) {
    return FluentIterable
        .from(executableElement.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

  private GoalContextFactory() {
    throw new UnsupportedOperationException("no instances");
  }
}

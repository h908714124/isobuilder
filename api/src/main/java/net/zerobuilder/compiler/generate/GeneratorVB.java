package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorVB {

  static final Function<AbstractRegularGoalContext, BuilderMethod> goalToBuilderV
      = goal -> {
    AbstractRegularGoalDetails abstractRegularGoalDetails = goal.regularDetails();
    List<AbstractRegularStep> steps = goal.regularSteps();
    MethodSpec.Builder method = methodBuilder(goal.methodName())
        .returns(goal.contractType().nestedClass(steps.get(0).thisType))
        .addModifiers(abstractRegularGoalDetails.option.access.modifiers(STATIC));
    ParameterSpec builder = builderInstance(goal);
    BuildersContext context = goal.context();
    ParameterSpec instance = parameterSpec(context.type, downcase(context.type.simpleName()));
    method.addCode(initBuilder(builder, instance).apply(goal));
    if (goal.isInstance()) {
      method.addParameter(instance);
    }
    MethodSpec methodSpec = method.addStatement("return $N", builder).build();
    return new BuilderMethod(goal.name(), methodSpec);
  };

  private static Function<AbstractRegularGoalContext, CodeBlock> initBuilder(
      ParameterSpec builder, ParameterSpec instance) {
    return regularGoalContextCases(
        initConstructorBuilder(builder),
        initMethodBuilder(builder, instance));
  }

  private static Function<ConstructorGoalContext, CodeBlock> initConstructorBuilder(
      ParameterSpec builder) {
    return cGoal -> {
      BuildersContext context = cGoal.context;
      TypeName type = builder.type;
      FieldSpec cache = context.cache.get();
      return context.lifecycle == REUSE_INSTANCES ?
          statement("$T $N = $N.get().$N", type, builder, cache, cGoal.cacheField()) :
          statement("$T $N = new $T()", type, builder, type);
    };
  }

  private static Function<MethodGoalContext, CodeBlock> initMethodBuilder(
      ParameterSpec builder, ParameterSpec instance) {
    return mGoal -> mGoal.methodType() == INSTANCE_METHOD ?
        initInstanceMethodBuilder(mGoal, builder, instance) :
        initStaticMethodBuilder(mGoal, builder);
  }

  private static CodeBlock initInstanceMethodBuilder(
      MethodGoalContext mGoal, ParameterSpec builder, ParameterSpec instance) {
    BuildersContext context = mGoal.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        CodeBlock.builder()
            .addStatement("$T $N = $N.get().$N", type, builder, cache, mGoal.cacheField())
            .addStatement("$N.$N = $N", builder, mGoal.field(), instance)
            .build() :
        statement("$T $N = new $T($N)", type, builder, type, instance);
  }

  private static CodeBlock initStaticMethodBuilder(
      MethodGoalContext mGoal, ParameterSpec builder) {
    BuildersContext context = mGoal.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        statement("$T $N = $N.get().$N", type, builder, cache, mGoal.cacheField()) :
        statement("$T $N = new $T()", type, builder, type);
  }

  private static ParameterSpec builderInstance(AbstractRegularGoalContext goal) {
    ClassName type = goal.implType();
    return parameterSpec(type, downcase(type.simpleName()));
  }

  private GeneratorVB() {
    throw new UnsupportedOperationException("no instances");
  }
}

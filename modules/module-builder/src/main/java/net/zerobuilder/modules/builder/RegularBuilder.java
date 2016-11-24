package net.zerobuilder.modules.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.name;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Step.regularStepInterface;

public final class RegularBuilder implements RegularSimpleModule {

  private static final String moduleName = "builder";

  private List<TypeSpec> stepInterfaces(SimpleRegularGoalContext goal) {
    return IntStream.range(0, goal.description().parameters().size())
        .mapToObj(regularStepInterface(goal))
        .collect(toList());
  }

  private List<MethodSpec> steps(SimpleRegularGoalContext goal) {
    return IntStream.range(0, goal.description().parameters().size())
        .mapToObj(Builder.stepsV(goal))
        .collect(toList());
  }

  private final Function<SimpleRegularGoalContext, List<FieldSpec>> fields =
      Builder.fieldsV;

  private final Function<SimpleRegularGoalContext, BuilderMethod> goalToBuilder =
      Generator::builderMethodV;

  static ClassName implType(SimpleRegularGoalContext goal) {
    ClassName contract = contractType(goal);
    return contract.peerClass(contract.simpleName() + "Impl");
  }

  static String methodName(SimpleRegularGoalContext goal) {
    return name.apply(goal) + upcase(moduleName);
  }

  private TypeSpec defineBuilderImpl(SimpleRegularGoalContext goal) {
    return classBuilder(implType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps(goal))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(SimpleRegularGoalContext goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private static final Function<SimpleRegularGoalContext, MethodSpec> regularConstructor =
      regularGoalContextCases(
          constructor -> constructor(),
          method -> {
            if (method.context.lifecycle == REUSE_INSTANCES) {
              return constructor();
            }
            TypeName type = method.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(rawClassName(type).get().simpleName()));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", method.instanceField(), parameter)
                .build();
          },
          staticMethod -> constructor());

  private final Function<SimpleRegularGoalContext, MethodSpec> builderConstructor =
      regularConstructor;

  static FieldSpec cacheField(SimpleRegularGoalContext goal) {
    ClassName type = implType(goal);
    return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }


  List<ClassName> stepInterfaceTypes(SimpleRegularGoalContext goal) {
    return transform(goal.description().parameters(),
        step -> contractType(goal).nestedClass(upcase(step.name)));
  }

  static ClassName contractType(SimpleRegularGoalContext goal) {
    String contractName = upcase(name.apply(goal)) + upcase(moduleName);
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(SimpleRegularGoalContext goal) {
    return new ModuleOutput(
        goalToBuilder.apply(goal),
        asList(
            defineBuilderImpl(goal),
            defineContract(goal)),
        singletonList(cacheField(goal)));
  }
}

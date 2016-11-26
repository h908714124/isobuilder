package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.instanceTypeParameters;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.Generator.updaterMethod;

public final class RegularUpdater implements ProjectedModule {

  private static final String moduleName = "updater";

  private MethodSpec buildMethod(ProjectedRegularGoalContext goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .addExceptions(goal.description().thrownTypes())
        .returns(goal.description().details().type())
        .addCode(regularInvoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(ProjectedRegularGoalContext projectedGoal) {
    return classBuilder(rawClassName(implType(projectedGoal)).get())
        .addFields(Updater.fields(projectedGoal))
        .addMethods(Updater.stepMethods(projectedGoal))
        .addTypeVariables(instanceTypeParameters.apply(projectedGoal))
        .addMethod(buildMethod(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(RegularUpdater.regularConstructor.apply(projectedGoal))
        .build();
  }

  static TypeName implType(ProjectedRegularGoalContext goal) {
    String implName = upcase(goal.description().details().name()) + upcase(moduleName);
    return parameterizedTypeName(
        goal.context().generatedType.nestedClass(implName),
        instanceTypeParameters.apply(goal));
  }

  private static final Function<ProjectedRegularGoalContext, MethodSpec> regularConstructor =
      projectedRegularGoalContextCases(
          method -> constructor(PRIVATE),
          constructor -> constructor(PRIVATE));

  private final Function<ProjectedRegularGoalContext, CodeBlock> regularInvoke =
      projectedRegularGoalContextCases(
          this::staticCall,
          this::constructorCall);

  private CodeBlock staticCall(ProjectedMethodGoalContext goal) {
    String method = goal.details.methodName;
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, goal.context.type,
            method, goal.invocationParameters())
        .add(free(goal.description().parameters()))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedConstructorGoalContext goal) {
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(rawClassName(type).get().simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type, goal.invocationParameters())
        .add(free(goal.description().parameters()))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock free(List<ProjectedParameter> steps) {
    return steps.stream()
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  static String methodName(ProjectedRegularGoalContext goal) {
    return goal.description().details().name() + upcase(moduleName);
  }

  static FieldSpec cacheField(ProjectedRegularGoalContext projectedGoal) {
    TypeName type = implType(projectedGoal);
    return FieldSpec.builder(type, downcase(rawClassName(type).get().simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }

  @Override
  public ModuleOutput process(ProjectedRegularGoalContext goal) {
    return new ModuleOutput(
        updaterMethod(goal),
        singletonList(defineUpdater(goal)),
        singletonList(cacheField(goal)));
  }
}

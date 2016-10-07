package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.THREAD_LOCAL;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;

public final class DtoBuildersContext {

  public static final class BuildersContext {

    /**
     * Whether builder instances should be cached.
     */
    final boolean recycle;

    /**
     * The type that should be generated.
     */
    final ClassName generatedType;

    /**
     * The class that contains the goal method(s) or constructor(s).
     * Only used in regular goals.
     */
    final ClassName type;

    /**
     * An instance of {@link #type}.
     * Only used in method goals, where {@link DtoGoal.MethodGoalDetails#instance} is true.
     */
    final FieldSpec field;

    /**
     * An instance of {@code ThreadLocal} that holds an instance of {@link #generatedType}.
     * Only used when {@link #recycle} is true.
     */
    final FieldSpec cache;

    private BuildersContext(boolean recycle, ClassName type, ClassName generatedType,
                            FieldSpec field, FieldSpec cache) {
      this.recycle = recycle;
      this.type = type;
      this.generatedType = generatedType;
      this.field = field;
      this.cache = cache;
    }
  }

  /**
   * Create metadata for goal processing.
   *
   * @param type          type that contains the goal methods / constructors;
   *                      for bean goals, this is just the bean type
   * @param generatedType type name that should be generated
   * @param recycle       if builder instances should be cached
   * @return a BuildersContext
   */
  public static BuildersContext createBuildersContext(ClassName type, ClassName generatedType, boolean recycle) {
    FieldSpec field = fieldSpec(type, '_' + downcase(type.simpleName()), PRIVATE);
    FieldSpec cache = defineCache(generatedType);
    return new BuildersContext(recycle, type, generatedType, field, cache);
  }

  private static FieldSpec defineCache(ClassName generatedType) {
    ParameterizedTypeName type = ParameterizedTypeName.get(THREAD_LOCAL, generatedType);
    TypeSpec initializer = anonymousClassBuilder("")
        .addSuperinterface(type)
        .addMethod(methodBuilder("initialValue")
            .addAnnotation(Override.class)
            .addModifiers(PROTECTED)
            .returns(generatedType)
            .addStatement("return new $T()", generatedType)
            .build())
        .build();
    return FieldSpec.builder(type, "INSTANCE")
        .initializer("$L", initializer)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private DtoBuildersContext() {
    throw new UnsupportedOperationException("no instances");
  }
}

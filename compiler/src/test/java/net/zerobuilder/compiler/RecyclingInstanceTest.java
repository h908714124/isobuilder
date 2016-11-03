package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingInstanceTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Sum",
        "package cube;",
        "import net.zerobuilder.*;",
        "",
        "@Builders(recycle = true)",
        "final class Sum {",
        "  private final int a;",
        "  @Goal(name = \"sum\") int sum(int b) { return a  + b; };",
        "  Sum (int a) { this.a = a; }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.SumBuilders",
            "package cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class SumBuilders {",
            "  private static final ThreadLocal<SumBuilders> INSTANCE = new ThreadLocal<SumBuilders>() {",
            "    @Override",
            "    protected SumBuilders initialValue() {",
            "      return new SumBuilders();",
            "    }",
            "  }",
            "",
            "  private SumBuilderImpl sumBuilderImpl = new SumBuilderImpl();",
            "",
            "  private SumBuilders() {}",
            "",
            "  public static SumBuilder.B sumBuilder(Sum sum) {",
            "    SumBuilders context = INSTANCE.get();",
            "    if (context.sumBuilderImpl._currently_in_use) {",
            "      context.sumBuilderImpl = new SumBuilderImpl();",
            "    }",
            "    context.sumBuilderImpl._currently_in_use = true;",
            "    context.sumBuilderImpl._sum = sum;",
            "    return context.sumBuilderImpl;",
            "  }",
            "",
            "  private static final class SumBuilderImpl implements SumBuilder.B {",
            "    private Sum _sum;",
            "    private boolean _currently_in_use;",
            "    StepsImpl() {}",
            "    @Override public int b(int b) {",
            "      this._currently_in_use = false;",
            "      int integer = this._sum.sum( b );",
            "      this._sum = null;",
            "      return integer;",
            "    }",
            "  }",
            "",
            "  public static final class SumBuilder {",
            "    private SumBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface B { int b(int b); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}

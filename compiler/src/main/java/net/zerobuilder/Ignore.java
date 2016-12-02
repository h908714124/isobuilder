package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks a &quot;getter&quot; method as ignored.
 * This may be necessary a method that looks like a &quot;getter&quot;
 * has no corresponding &quot;setter&quot;.
 * </p><p>
 * <em>Note:</em> Only applies to &quot;bean goals&quot;.
 * </p>
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface Ignore {
}

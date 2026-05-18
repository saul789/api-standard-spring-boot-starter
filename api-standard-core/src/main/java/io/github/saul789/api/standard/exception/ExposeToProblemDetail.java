package io.github.saul789.api.standard.exception;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field inside an Exception should be exposed in the final
 * ProblemDetail JSON response.
 *
 * <p>This annotation follows a "Secure by Default" (opt-in) approach. Fields are
 * NEVER exposed to the client unless explicitly annotated with this.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExposeToProblemDetail {
    
    /**
     * Optional custom name for the field in the ProblemDetail JSON.
     * If left blank, the original field name will be used.
     *
     * @return The custom property name to use
     */
    String value() default "";
}

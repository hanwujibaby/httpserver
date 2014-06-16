package cannon.server.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
	String value() ;
	boolean required() default true;
	String defaultValue() default ValueConstants.DEFAULT_NONE;
}

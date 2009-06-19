package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Getter {
	lombok.AccessLevel DEFAULT_ACCESS_LEVEL = lombok.AccessLevel.PUBLIC;
	
	lombok.AccessLevel value() default lombok.AccessLevel.PUBLIC;
}

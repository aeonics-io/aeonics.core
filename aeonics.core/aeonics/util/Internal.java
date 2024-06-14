package aeonics.util;

import java.lang.annotation.*;

/**
 * This annotation is used to specify that some classes, methods or else are considered internal and should not be used 
 * even though for practical reasons they may be declared as public or protected.
 * It is heavily recommended to also include the {@literal @hidden} javadoc tag to skip public documentation which may mislead users.
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.SOURCE)
public @interface Internal { }

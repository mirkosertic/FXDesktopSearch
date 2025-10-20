package de.mirkosertic.desktopsearch;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Scope("prototype")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JavaFXController {
}

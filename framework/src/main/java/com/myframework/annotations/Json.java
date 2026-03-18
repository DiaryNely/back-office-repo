package com.myframework.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation permettant d'indiquer qu'une méthode de contrôleur
 * doit renvoyer une réponse JSON au lieu d'une vue JSP.
 *
 * Sprint 9 – API REST Support
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Json {
}

package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Sprint 11 - Annotation pour injecter une valeur depuis la session HTTP
 * 
 * Exemple:
 *   @GetMapping("/profile")
 *   public ModelView profile(@SessionAttribute("user") User currentUser) {
 *       // currentUser est récupéré depuis session.getAttribute("user")
 *   }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SessionAttribute {
    /**
     * Le nom de l'attribut dans la session
     */
    String value();
    
    /**
     * Si true, une exception est levée si l'attribut n'existe pas
     * Si false, null est retourné
     */
    boolean required() default true;
}

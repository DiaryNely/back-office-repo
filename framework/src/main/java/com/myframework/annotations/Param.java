package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Annotation pour specifier le nom d'un parametre de requete
 * 
 * Exemple:
 * public ModelView search(@Param("q") String query, @Param("page") int page) {
 *     // query vient de request.getParameter("q")
 *     // page vient de request.getParameter("page")
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
    /**
     * Le nom du parametre dans la requete HTTP
     */
    String value();
}
// annotations/FileParam.java
package com.myframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sprint 10 : Annotation pour injecter un fichier uploadé dans un paramètre
 * 
 * Exemple:
 *   @PostMapping("/upload")
 *   public String upload(@FileParam("photo") UploadedFile file) { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FileParam {
    /**
     * Nom du champ dans le formulaire HTML
     */
    String value();
}
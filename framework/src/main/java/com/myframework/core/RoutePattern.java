package com.myframework.core;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe pour gerer les patterns d'URL avec parametres dynamiques
 * 
 * Exemples:
 *   /users/{id}        -> match /users/123, /users/abc
 *   /product/{name}    -> match /product/laptop, /product/phone
 *   /blog/{year}/{month} -> match /blog/2024/12
 */
public class RoutePattern {
    
    private String originalPattern;  // Pattern original: /users/{id}
    private Pattern regexPattern;    // Pattern regex compile
    private String[] paramNames;     // Noms des parametres: ["id"]
    
    /**
     * Constructeur
     * @param pattern Le pattern avec parametres (ex: "/users/{id}")
     */
    public RoutePattern(String pattern) {
        this.originalPattern = pattern;
        this.paramNames = extractParamNames(pattern);
        this.regexPattern = compilePattern(pattern);
    }
    
    /**
     * Extraire les noms des parametres du pattern
     * Ex: "/users/{id}/posts/{postId}" -> ["id", "postId"]
     */
    private String[] extractParamNames(String pattern) {
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = paramPattern.matcher(pattern);
        
        java.util.List<String> names = new java.util.ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        
        return names.toArray(new String[0]);
    }
    
    /**
     * Compiler le pattern en regex
     * Ex: "/users/{id}" -> "^/users/([^/]+)$"
     */
    private Pattern compilePattern(String pattern) {
        // Remplacer {param} par un groupe de capture regex
        String regex = pattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
        // Ajouter ancres debut et fin
        regex = "^" + regex + "$";
        return Pattern.compile(regex);
    }
    
    /**
     * Verifier si une URL correspond au pattern
     * @param url L'URL a tester
     * @return true si l'URL correspond
     */
    public boolean matches(String url) {
        return regexPattern.matcher(url).matches();
    }
    
    /**
     * Extraire les parametres d'une URL
     * @param url L'URL a analyser
     * @return Map des parametres (nom -> valeur) ou null si pas de match
     */
    public Map<String, String> extractParams(String url) {
        Matcher matcher = regexPattern.matcher(url);
        
        if (!matcher.matches()) {
            return null;
        }
        
        Map<String, String> params = new HashMap<>();
        
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            String paramValue = matcher.group(i + 1);
            params.put(paramName, paramValue);
        }
        
        return params;
    }
    
    /**
     * Obtenir le pattern original
     */
    public String getOriginalPattern() {
        return originalPattern;
    }
    
    /**
     * Obtenir les noms des parametres
     */
    public String[] getParamNames() {
        return paramNames;
    }
    
    @Override
    public String toString() {
        return "RoutePattern{pattern='" + originalPattern + "', params=" + java.util.Arrays.toString(paramNames) + "}";
    }
}
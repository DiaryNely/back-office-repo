package com.myframework.core;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Classe qui represente une route (pattern + methode + methode HTTP)
 */
public class RouteMapping {
    
    private RoutePattern pattern;  // Le pattern de l'URL
    private Method method;         // La methode a appeler
    private boolean hasParams;     // Est-ce que le pattern a des parametres?
    private String httpMethod;     // La methode HTTP (GET, POST, PUT, DELETE, etc.)
    
    /**
     * Constructeur
     * @param urlPattern Le pattern de l'URL (ex: "/users/{id}")
     * @param method La methode a appeler
     * @param httpMethod La methode HTTP (GET, POST, etc.)
     */
    public RouteMapping(String urlPattern, Method method, String httpMethod) {
        this.pattern = new RoutePattern(urlPattern);
        this.method = method;
        this.hasParams = urlPattern.contains("{");
        this.httpMethod = httpMethod;
    }
    
    /**
     * Constructeur pour compatibilite (par defaut accepte toutes les methodes)
     */
    public RouteMapping(String urlPattern, Method method) {
        this(urlPattern, method, null);
    }
    
    /**
     * Verifier si une URL ET une methode HTTP correspondent
     */
    public boolean matches(String url, String httpMethod) {
        boolean urlMatches = pattern.matches(url);
        
        // Si pas de methode HTTP specifiee, accepter toutes les methodes
        if (this.httpMethod == null) {
            return urlMatches;
        }
        
        // Sinon, verifier que la methode HTTP correspond
        return urlMatches && this.httpMethod.equalsIgnoreCase(httpMethod);
    }
    
    /**
     * Extraire les parametres d'une URL
     */
    public Map<String, String> extractParams(String url) {
        return pattern.extractParams(url);
    }
    
    /**
     * Obtenir le pattern
     */
    public RoutePattern getPattern() {
        return pattern;
    }
    
    /**
     * Obtenir la methode
     */
    public Method getMethod() {
        return method;
    }
    
    /**
     * Est-ce que cette route a des parametres?
     */
    public boolean hasParams() {
        return hasParams;
    }
    
    /**
     * Obtenir le pattern original
     */
    public String getOriginalPattern() {
        return pattern.getOriginalPattern();
    }
    
    /**
     * Obtenir la methode HTTP
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    
    @Override
    public String toString() {
        String methodStr = httpMethod != null ? httpMethod : "ALL";
        return "RouteMapping{pattern='" + pattern.getOriginalPattern() + 
               "', method=" + method.getName() + 
               ", httpMethod=" + methodStr +
               ", hasParams=" + hasParams + "}";
    }
}
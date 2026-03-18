package com.myframework.utils;

import jakarta.servlet.ServletContext;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Classe utilitaire pour acceder aux routes stockees dans le ServletContext
 */
public class RouteHelper {
    
    private static final String ROUTES_KEY = "framework.routes";
    
    /**
     * Recuperer toutes les routes depuis le ServletContext
     * 
     * @param context Le ServletContext
     * @return Map des routes (URL -> Method)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Method> getRoutes(ServletContext context) {
        return (Map<String, Method>) context.getAttribute(ROUTES_KEY);
    }
    
    /**
     * Recuperer une route specifique
     * 
     * @param context Le ServletContext
     * @param url L'URL de la route
     * @return La methode associee ou null
     */
    public static Method getRoute(ServletContext context, String url) {
        Map<String, Method> routes = getRoutes(context);
        if (routes == null) {
            return null;
        }
        return routes.get(url);
    }
    
    /**
     * Verifier si une route existe
     * 
     * @param context Le ServletContext
     * @param url L'URL a verifier
     * @return true si la route existe
     */
    public static boolean routeExists(ServletContext context, String url) {
        return getRoute(context, url) != null;
    }
    
    /**
     * Afficher toutes les routes (pour debug)
     * 
     * @param context Le ServletContext
     */
    public static void printRoutes(ServletContext context) {
        Map<String, Method> routes = getRoutes(context);
        if (routes == null || routes.isEmpty()) {
            System.out.println("Aucune route dans le ServletContext");
            return;
        }
        
        System.out.println("Routes disponibles dans ServletContext:");
        for (Map.Entry<String, Method> entry : routes.entrySet()) {
            String url = entry.getKey();
            Method method = entry.getValue();
            System.out.println("  " + url + " -> " + 
                             method.getDeclaringClass().getSimpleName() + "." + 
                             method.getName() + "()");
        }
    }
}
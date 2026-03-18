package com.myframework.core;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 11 - Wrapper autour de HttpSession pour simplifier l'utilisation
 * 
 * Permet aux développeurs de manipuler la session comme une Map<String, Object>
 * 
 * Exemple dans un contrôleur:
 *   public ModelView login(MySession session) {
 *       session.put("user", user);
 *       session.put("userRole", "admin");
 *   }
 */
public class MySession {
    
    private final HttpSession httpSession;
    
    public MySession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }
    
    /**
     * Ajouter une valeur dans la session
     */
    public void put(String key, Object value) {
        httpSession.setAttribute(key, value);
    }
    
    /**
     * Récupérer une valeur de la session
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) httpSession.getAttribute(key);
    }
    
    /**
     * Récupérer une valeur avec un type spécifique
     */
    public <T> T get(String key, Class<T> type) {
        Object value = httpSession.getAttribute(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    /**
     * Vérifier si une clé existe dans la session
     */
    public boolean contains(String key) {
        return httpSession.getAttribute(key) != null;
    }
    
    /**
     * Supprimer une valeur de la session
     */
    public void remove(String key) {
        httpSession.removeAttribute(key);
    }
    
    /**
     * Vider complètement la session (logout)
     */
    public void invalidate() {
        httpSession.invalidate();
    }
    
    /**
     * Obtenir l'ID de la session
     */
    public String getId() {
        return httpSession.getId();
    }
    
    /**
     * Obtenir la session HTTP native
     */
    public HttpSession getHttpSession() {
        return httpSession;
    }
    
    /**
     * Convertir la session en Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        var names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, httpSession.getAttribute(name));
        }
        return map;
    }
    
    @Override
    public String toString() {
        return "MySession{id=" + getId() + ", data=" + toMap() + "}";
    }
}

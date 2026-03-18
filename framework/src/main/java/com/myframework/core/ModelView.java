package com.myframework.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe qui represente une vue (JSP) a afficher avec des donnees
 * 
 * Utilisation:
 *   ModelView mv = new ModelView("test.jsp");
 *   mv.addData("nom", "Jean");
 *   mv.addData("age", 25);
 *   return mv;
 */
public class ModelView {
    
    private String view;  // Nom du fichier JSP
    private Map<String, Object> data;  // Donnees a envoyer au JSP
    
    /**
     * Constructeur
     * @param view Le nom du fichier JSP (ex: "test.jsp", "user/list.jsp")
     */
    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }
    
    /**
     * Recuperer le nom de la vue
     * @return Le nom du fichier JSP
     */
    public String getView() {
        return view;
    }
    
    /**
     * Modifier le nom de la vue
     * @param view Le nouveau nom de fichier JSP
     */
    public void setView(String view) {
        this.view = view;
    }
    
    /**
     * Recuperer toutes les donnees
     * @return La map des donnees
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Ajouter une donnee
     * @param key La cle de la donnee
     * @param value La valeur de la donnee
     * @return Le ModelView (pour chainage)
     */
    public ModelView addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
    
    /**
     * Ajouter plusieurs donnees
     * @param data Map des donnees a ajouter
     * @return Le ModelView (pour chainage)
     */
    public ModelView addAllData(Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }
     public ModelView() {
        this.data = new HashMap<>();
    }
    
    /**
     * Recuperer une donnee specifique
     * @param key La cle de la donnee
     * @return La valeur ou null
     */
    public Object getData(String key) {
        return this.data.get(key);
    }
    
    /**
     * Verifier si une donnee existe
     * @param key La cle a verifier
     * @return true si la donnee existe
     */
    public boolean hasData(String key) {
        return this.data.containsKey(key);
    }
    
    @Override
    public String toString() {
        return "ModelView{view='" + view + "', data=" + data + "}";
    }
}
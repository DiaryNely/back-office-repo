package com.myframework.utils;

import com.myframework.annotations.Param;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour resoudre les parametres d'une methode
 * et les injecter depuis la requete HTTP
 */
public class ParameterResolver {
    
    /**
     * Resoudre tous les parametres d'une methode
     * 
     * @param method La methode a analyser
     * @param request La requete HTTP
     * @param response La reponse HTTP
     * @return Tableau des valeurs a passer a la methode
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest request, HttpServletResponse response) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> paramType = parameter.getType();
            
            // 1. Si c'est HttpServletRequest
            if (paramType.equals(HttpServletRequest.class)) {
                args[i] = request;
                continue;
            }
            
            // 2. Si c'est HttpServletResponse
            if (paramType.equals(HttpServletResponse.class)) {
                args[i] = response;
                continue;
            }
            
            // 3. Si c'est un parametre avec @Param
            if (parameter.isAnnotationPresent(Param.class)) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                String paramName = paramAnnotation.value();
                String paramValue = request.getParameter(paramName);
                
                // Convertir selon le type
                args[i] = convertParameter(paramValue, paramType, paramName);
                continue;
            }
            
            // 4. Sinon, utiliser le nom du parametre (si disponible avec -parameters)
            String paramName = parameter.getName();
            String paramValue = request.getParameter(paramName);
            args[i] = convertParameter(paramValue, paramType, paramName);
        }
        
        return args;
    }
    
    /**
     * Convertir une valeur String en type approprie
     */
    private static Object convertParameter(String value, Class<?> targetType, String paramName) {
        // Si la valeur est null ou vide
        if (value == null || value.isEmpty()) {
            return getDefaultValue(targetType);
        }
        
        try {
            // String
            if (targetType.equals(String.class)) {
                return value;
            }
            
            // int / Integer
            if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
                return Integer.parseInt(value);
            }
            
            // long / Long
            if (targetType.equals(long.class) || targetType.equals(Long.class)) {
                return Long.parseLong(value);
            }
            
            // double / Double
            if (targetType.equals(double.class) || targetType.equals(Double.class)) {
                return Double.parseDouble(value);
            }
            
            // float / Float
            if (targetType.equals(float.class) || targetType.equals(Float.class)) {
                return Float.parseFloat(value);
            }
            
            // boolean / Boolean
            if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
                return Boolean.parseBoolean(value);
            }
            
            // Par defaut, retourner la valeur String
            return value;
            
        } catch (NumberFormatException e) {
            System.err.println("Erreur de conversion du parametre '" + paramName + "' : " + value + " vers " + targetType.getSimpleName());
            return getDefaultValue(targetType);
        }
    }
    
    /**
     * Obtenir la valeur par defaut pour un type primitif
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type.equals(int.class)) return 0;
        if (type.equals(long.class)) return 0L;
        if (type.equals(double.class)) return 0.0;
        if (type.equals(float.class)) return 0.0f;
        if (type.equals(boolean.class)) return false;
        return null;
    }
    
    /**
     * Afficher les parametres resolus (pour debug)
     */
    public static void logParameters(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        System.out.println("  -> Parametres de la methode:");
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            
            if (param.isAnnotationPresent(Param.class)) {
                paramName = param.getAnnotation(Param.class).value();
            }
            
            String typeName = param.getType().getSimpleName();
            Object value = args[i];
            
            if (value instanceof HttpServletRequest) {
                System.out.println("      * " + paramName + " : HttpServletRequest");
            } else if (value instanceof HttpServletResponse) {
                System.out.println("      * " + paramName + " : HttpServletResponse");
            } else {
                System.out.println("      * " + paramName + " : " + typeName + " = " + value);
            }
        }
    }
}
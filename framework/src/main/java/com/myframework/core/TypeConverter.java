package com.myframework.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TypeConverter {

    public static Object convert(String value, Class<?> targetType) {

        if (value == null || value.isEmpty())
            return null;

        try {

            // --- STRING ---
            if (targetType == String.class)
                return value;

            // --- INTEGER ---
            if (targetType == int.class || targetType == Integer.class)
                return Integer.parseInt(value);

            // --- LONG ---
            if (targetType == long.class || targetType == Long.class)
                return Long.parseLong(value);

            // --- DOUBLE ---
            if (targetType == double.class || targetType == Double.class)
                return Double.parseDouble(value);

            // --- FLOAT ---
            if (targetType == float.class || targetType == Float.class)
                return Float.parseFloat(value);

            // --- SHORT ---
            if (targetType == short.class || targetType == Short.class)
                return Short.parseShort(value);

            // --- BOOLEAN ---
            if (targetType == boolean.class || targetType == Boolean.class)
                return Boolean.parseBoolean(value);

            // --- ENUM ---
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, value);
            }

            // --- LocalDate (format: yyyy-MM-dd) ---
            if (targetType == LocalDate.class) {
                return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            }

            // --- LocalDateTime (format: yyyy-MM-ddTHH:mm:ss) ---
            if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            }

            // --- Autres types : retourner brut ---
            return value;

        } catch (Exception e) {
            System.err.println("❌ Erreur TypeConverter : impossible de convertir '" 
                    + value + "' en " + targetType.getSimpleName());

            return null; // version simple
        }
    }
}

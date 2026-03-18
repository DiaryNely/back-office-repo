package com.myframework.core;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectBinder {

    /**
     * Reconstruit un objet complexe à partir de la paramMap.
     * Exemple de prefix :
     *   - "e"  → e.name, e.department[0].name
     *   - "d"  → d.name, d.id
     */
    public static Object bindObject(Class<?> type, Map<String, Object> paramMap, String prefix) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();

            Field[] fields = type.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                String fieldPrefix = prefix + "." + field.getName(); // e.name, e.department

                // 1️⃣ Champ simple (String, int, double, boolean, etc.)
                if (!fieldType.isArray() && !isCustomClass(fieldType)) {
                    if (paramMap.containsKey(fieldPrefix)) {
                        Object raw = paramMap.get(fieldPrefix);

                        Object converted = TypeConverter.convert(
                                raw != null ? raw.toString() : null,
                                fieldType
                        );

                        field.set(instance, converted);
                    }
                }

                // 2️⃣ Champ objet imbriqué (ex: Employee.department)
                else if (!fieldType.isArray() && isCustomClass(fieldType)) {
                    Object nested = bindObject(fieldType, paramMap, fieldPrefix);
                    field.set(instance, nested);
                }

                // 3️⃣ Champ tableau (ex: Department[] department)
                else if (fieldType.isArray()) {
                    Class<?> componentType = fieldType.getComponentType();
                    Object array = bindArrayFromPrefix(componentType, paramMap, fieldPrefix);
                    field.set(instance, array);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Erreur pendant le binding de " + type.getSimpleName(), e);
        }
    }

    /**
     * Construit un tableau à partir des clés du type:
     *   e.department[0].name
     *   e.department[1].name
     *
     * prefix = "e.department"
     */
    public static Object bindArrayFromPrefix(Class<?> componentType,
                                             Map<String, Object> paramMap,
                                             String prefix) {

        List<Object> elements = new ArrayList<>();
        int index = 0;

        while (true) {
            String indexedPrefix = prefix + "[" + index + "]";
            boolean found = false;

            // Vérifier s'il existe AU MOINS une clé qui commence par "e.department[0]"
            for (String key : paramMap.keySet()) {
                if (key.startsWith(indexedPrefix)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                break; // pas d’autres éléments
            }

            Object value;

            // Cas 1 : tableau d’objets (Department[], Address[], ...)
            if (isCustomClass(componentType)) {
                value = bindObject(componentType, paramMap, indexedPrefix);
            }

            // Cas 2 : tableau simple (String[], int[], etc.)
            else {
                Object raw = paramMap.get(indexedPrefix);

                value = TypeConverter.convert(
                        raw != null ? raw.toString() : null,
                        componentType
                );
            }

            elements.add(value);
            index++;
        }

        // Création du tableau final
        Object array = Array.newInstance(componentType, elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Array.set(array, i, elements.get(i));
        }

        return array;
    }

    /**
     * Détection d'une classe "métier" custom (Employee, Department, etc.)
     */
    public static boolean isCustomClass(Class<?> type) {
        return !type.isPrimitive()
                && !type.getName().startsWith("java.")
                && !type.isArray();
    }
}

package com.myframework.serializer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class JsonSerializer {

    // ================================
    // MÉTHODE PRINCIPALE
    // ================================
    public static String toJson(Object obj) {
        if (obj == null)
            return "null";

        if (obj instanceof String)
            return "\"" + escape((String) obj) + "\"";

        if (obj instanceof Number || obj instanceof Boolean)
            return obj.toString();

        // Gestion des types temporels Java (LocalDateTime, LocalDate, LocalTime, etc.)
        if (obj instanceof Temporal)
            return "\"" + obj.toString() + "\"";

        if (obj instanceof Date)
            return "\"" + obj.toString() + "\"";

        if (obj instanceof Map)
            return mapToJson((Map<?, ?>) obj);

        if (obj instanceof Collection)
            return collectionToJson((Collection<?>) obj);

        if (obj.getClass().isArray())
            return arrayToJson(obj);

        // Enum
        if (obj instanceof Enum)
            return "\"" + obj.toString() + "\"";

        // Objet Java (POJO)
        return objectToJson(obj);
    }

    // ================================
    // Échappement des guillemets et caractères spéciaux
    // ================================
    private static String escape(String str) {
        return str.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }


    // ================================
    // Map → JSON
    // ================================
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");

        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;

            sb.append("\"")
              .append(escape(entry.getKey().toString()))
              .append("\":")
              .append(toJson(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }


    // ================================
    // Collection → JSON
    // ================================
    private static String collectionToJson(Collection<?> col) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (Object o : col) {
            if (!first) sb.append(",");
            first = false;
            sb.append(toJson(o));
        }

        sb.append("]");
        return sb.toString();
    }


    // ================================
    // Array → JSON
    // ================================
    private static String arrayToJson(Object array) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(array);

        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(Array.get(array, i)));
        }

        sb.append("]");
        return sb.toString();
    }


    // ================================
    // Objet Java (POJO) → JSON
    // ================================
    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");

        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field f : fields) {
            try {
                f.setAccessible(true);
                Object value = f.get(obj);

                if (!first) sb.append(",");
                first = false;

                sb.append("\"")
                  .append(f.getName())
                  .append("\":")
                  .append(toJson(value));

            } catch (Exception ignored) {}
        }

        sb.append("}");
        return sb.toString();
    }
}

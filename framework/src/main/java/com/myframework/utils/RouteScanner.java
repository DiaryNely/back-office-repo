package com.myframework.utils;

import com.myframework.annotations.*;
import com.myframework.core.RouteMapping;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class RouteScanner {

    public static List<RouteMapping> scanRoutes(String packageName) {
        List<RouteMapping> routes = new ArrayList<>();

        System.out.println("========================================================");
        System.out.println("Début du scan des routes dans le package : " + packageName);
        System.out.println("========================================================");

        try {
            List<Class<?>> classes = getClasses(packageName);
            System.out.println("Nombre de classes trouvées : " + classes.size());

            int controllersFound = 0;
            int routesFound = 0;

            for (Class<?> clazz : classes) {

                if (!clazz.isAnnotationPresent(Controller.class)) {
                    System.out.println("  [IGNORE] " + clazz.getSimpleName() + " (pas de @Controller)");
                    continue;
                }

                controllersFound++;
                System.out.println("  [CONTROLLER] " + clazz.getName());

                for (Method method : clazz.getDeclaredMethods()) {
                    String urlPattern = null;
                    String httpMethod = null;

                    // GET
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        urlPattern = method.getAnnotation(GetMapping.class).value();
                        httpMethod = "GET";
                    }
                    // POST
                    else if (method.isAnnotationPresent(PostMapping.class)) {
                        urlPattern = method.getAnnotation(PostMapping.class).value();
                        httpMethod = "POST";
                    }
                    // WebRoute = toutes méthodes
                    else if (method.isAnnotationPresent(WebRoute.class)) {
                        urlPattern = method.getAnnotation(WebRoute.class).value();
                        httpMethod = null; // ALL
                    }

                    if (urlPattern != null) {
                        RouteMapping mapping = new RouteMapping(urlPattern, method, httpMethod);
                        routes.add(mapping);
                        routesFound++;

                        String methodStr = httpMethod != null ? "[" + httpMethod + "]" : "[ALL]";
                        String typeStr = mapping.hasParams() ? "[DYNAMIC]" : "[STATIC]";

                        System.out.println("    -> " + methodStr + " " + typeStr + " " + urlPattern + " => " + method.getName() + "()");
                    }
                }
            }

            System.out.println("========================================================");
            System.out.println("Scan terminé :");
            System.out.println("  - Contrôleurs trouvés : " + controllersFound);
            System.out.println("  - Routes totales : " + routesFound);
            System.out.println("========================================================");

        } catch (Exception e) {
            System.err.println("ERREUR lors du scan des routes : " + e.getMessage());
            e.printStackTrace();
        }

        return routes;
    }

    @Deprecated
    public static Map<String, Method> scanRoutesLegacy(String packageName) {
        List<RouteMapping> allRoutes = scanRoutes(packageName);
        Map<String, Method> staticRoutes = new HashMap<>();

        for (RouteMapping mapping : allRoutes) {
            if (!mapping.hasParams()) {
                staticRoutes.put(mapping.getOriginalPattern(), mapping.getMethod());
            }
        }

        return staticRoutes;
    }

    private static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());

            if (directory.exists()) {
                classes.addAll(findClasses(directory, packageName));
            }
        }

        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();

        if (!directory.exists()) return classes;

        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }
}

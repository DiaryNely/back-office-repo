package com.myframework.servlet;

import com.myframework.annotations.Json;
import com.myframework.annotations.Param;
import com.myframework.annotations.RequestParam;
import com.myframework.annotations.FileParam;
import com.myframework.annotations.SessionAttribute;
import com.myframework.annotations.Auth;
import com.myframework.core.ModelView;
import com.myframework.core.ObjectBinder;
import com.myframework.core.RouteMapping;
import com.myframework.core.TypeConverter;
import com.myframework.core.UploadedFile;
import com.myframework.core.MySession;
import com.myframework.http.JsonResponse;
import com.myframework.serializer.JsonSerializer;
import com.myframework.utils.RouteScanner;
import com.myframework.exceptions.UnauthorizedException;
import com.myframework.exceptions.ForbiddenException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.MultipartConfig;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Array;
import java.util.*;

@MultipartConfig
public class FrontServlet extends HttpServlet {

    private List<RouteMapping> routes;

    @Override
    public void init() throws ServletException {
        System.out.println("==============================================");
        System.out.println("        DEMARRAGE FRAMEWORK MVC");
        System.out.println("==============================================\n");

        String packageToScan = getInitParameter("controllerPackage");
        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            packageToScan = "com.test.controller";
        }
        System.out.println("Package à scanner : " + packageToScan);
        
        routes = RouteScanner.scanRoutes(packageToScan);

        getServletContext().setAttribute("framework.routes", routes);

        System.out.println("Routes trouvées : " + routes.size());
        for (RouteMapping r : routes) {
            String http = (r.getHttpMethod() == null) ? "[ALL]" : "[" + r.getHttpMethod() + "]";
            System.out.println(" " + http + " => " + r.getOriginalPattern());
        }
        System.out.println("==============================================\n");
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        try {
            processRequest(req, resp);
        }
        // Sprint 11 bis: Gestion erreur 401 (non authentifié)
        catch (UnauthorizedException ex) {
            System.out.println("🔒 ERREUR 401 : " + ex.getMessage());
            
            if (req.getAttribute("json_mode") != null) {
                JsonResponse errorJson = new JsonResponse("error", 401, ex.getMessage(), null);
                resp.setStatus(401);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write(JsonSerializer.toJson(errorJson));
            } else {
                render401(req, resp, ex.getMessage());
            }
        }
        // Sprint 11 bis: Gestion erreur 403 (accès interdit)
        catch (ForbiddenException ex) {
            System.out.println("🚫 ERREUR 403 : " + ex.getMessage());
            
            if (req.getAttribute("json_mode") != null) {
                JsonResponse errorJson = new JsonResponse("error", 403, ex.getMessage(), null);
                resp.setStatus(403);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write(JsonSerializer.toJson(errorJson));
            } else {
                render403(req, resp, ex.getMessage());
            }
        }
        catch (Exception ex) {

            System.out.println("🔥 ERREUR catch global : " + ex.getMessage());
            ex.printStackTrace();

            // Si demande JSON → on renvoie JSON d'erreur
            if (req.getAttribute("json_mode") != null) {
                JsonResponse errorJson = new JsonResponse(
                        "error",
                        500,
                        ex.getMessage(),
                        null
                );
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write(JsonSerializer.toJson(errorJson));
            }
            else {
                throw new ServletException(ex);
            }
        }
    }


    // ===================================================================
    // PROCESS REQUEST (avec Sprint 10 finalisé)
    // ===================================================================
    private void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod();

        System.out.println("\n[REQUEST] " + httpMethod + " " + path);

        // ---------- Static files ----------
        InputStream resource = getServletContext().getResourceAsStream(path);
        if (resource != null) {
            serveStaticFile(resp, resource, path);
            return;
        }

        // ---------- Find route ----------
        RouteMapping matchedRoute = null;
        Map<String, String> urlParams = null;

        for (RouteMapping r : routes) {
            if (r.matches(path, httpMethod)) {
                matchedRoute = r;
                urlParams = r.extractParams(path);
                break;
            }
        }

        if (matchedRoute == null) {
            render404(req, resp, path, httpMethod);
            return;
        }

        System.out.println("  -> Route trouvée : " + matchedRoute.getOriginalPattern());

        // ---------- Instantiate controller ----------
        Method method = matchedRoute.getMethod();
        Object controller = method.getDeclaringClass().getDeclaredConstructor().newInstance();

        // Set JSON mode flag
        if (method.isAnnotationPresent(Json.class)) {
            req.setAttribute("json_mode", true);
        }

        // ---------- Sprint 11 bis: Vérification sécurité AVANT exécution ----------
        checkSecurity(method, req);

        // ---------- Build paramMap (avec support multipart) ----------
        Map<String, Object> paramMap = new HashMap<>();

        // Vérifier si c'est multipart/form-data
        String contentType = req.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            // En multipart, les paramètres normaux sont aussi dans les Parts
            try {
                Collection<Part> parts = req.getParts();
                for (Part part : parts) {
                    // Si pas de filename → c'est un champ normal (pas un fichier)
                    if (part.getSubmittedFileName() == null || part.getSubmittedFileName().isEmpty()) {
                        String fieldName = part.getName();
                        try (InputStream is = part.getInputStream()) {
                            String fieldValue = new String(is.readAllBytes(), "UTF-8");
                            paramMap.put(fieldName, fieldValue);
                            System.out.println("  -> Champ multipart: " + fieldName + " = " + fieldValue);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur extraction champs multipart : " + e.getMessage());
            }
        } else {
            // Formulaire normal (application/x-www-form-urlencoded)
            Enumeration<String> names = req.getParameterNames();
            while (names.hasMoreElements()) {
                String pname = names.nextElement();
                paramMap.put(pname, req.getParameter(pname));
            }
        }

        // Ajouter les paramètres URL
        if (urlParams != null)
            paramMap.putAll(urlParams);

        System.out.println("  -> paramMap = " + paramMap);

        // ---------- Extract uploaded files (Sprint 10) ----------
        Map<String, UploadedFile> uploadedFiles = extractUploadedFiles(req);
        
        if (!uploadedFiles.isEmpty()) {
            System.out.println("  -> Fichiers uploadés : " + uploadedFiles.keySet());
        }

        // ---------- Resolve arguments ----------
        List<Object> args = new ArrayList<>();
        Parameter[] methodParams = method.getParameters();

        for (int i = 0; i < methodParams.length; i++) {
            args.add(resolveArgument(
                methodParams[i], i, req, resp, 
                matchedRoute, urlParams, paramMap, uploadedFiles
            ));
        }

        // ---------- Execute controller ----------
        Object result = method.invoke(controller, args.toArray());

        // ---------- Handle return ----------
        handleReturn(result, method, req, resp);
    }


    // ===================================================================
    // EXTRACT UPLOADED FILES (Sprint 10)
    // ===================================================================
    private Map<String, UploadedFile> extractUploadedFiles(HttpServletRequest req) 
            throws IOException, ServletException {
        
        Map<String, UploadedFile> files = new HashMap<>();

        // Vérifier si c'est une requête multipart
        String contentType = req.getContentType();
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return files; // Pas de fichiers
        }

        try {
            Collection<Part> parts = req.getParts();
            
            for (Part part : parts) {
                // Vérifier si c'est un fichier (et non un champ de formulaire)
                String submittedFileName = part.getSubmittedFileName();
                
                if (submittedFileName != null && !submittedFileName.isEmpty()) {
                    // C'est un fichier
                    String fieldName = part.getName();
                    String fileName = submittedFileName;
                    String mimeType = part.getContentType();
                    long fileSize = part.getSize();
                    
                    // Lire le contenu du fichier
                    byte[] content = part.getInputStream().readAllBytes();
                    
                    // Créer l'objet UploadedFile
                    UploadedFile uploadedFile = new UploadedFile(
                        fieldName, fileName, mimeType, fileSize, content
                    );
                    
                    files.put(fieldName, uploadedFile);
                    
                    System.out.println("  📁 Fichier détecté : " + fieldName + " = " + fileName 
                        + " (" + fileSize + " bytes)");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'extraction des fichiers : " + e.getMessage());
        }

        return files;
    }


    // ===================================================================
    // RESOLVE ARGUMENTS (avec Sprint 10 finalisé)
    // ===================================================================
    private Object resolveArgument(
            Parameter p, int index, 
            HttpServletRequest req, HttpServletResponse resp,
            RouteMapping route, 
            Map<String, String> urlParams, 
            Map<String, Object> paramMap,
            Map<String, UploadedFile> uploadedFiles
    ) {

        Class<?> targetType = p.getType();

        // Sprint 10: @FileParam pour injecter un fichier uploadé
        if (p.isAnnotationPresent(FileParam.class)) {
            String fieldName = p.getAnnotation(FileParam.class).value();
            
            UploadedFile file = uploadedFiles.get(fieldName);
            
            if (file == null) {
                System.out.println("  ⚠️ Aucun fichier trouvé pour @FileParam(\"" + fieldName + "\")");
            }
            
            // Injection selon le type demandé
            if (targetType == UploadedFile.class) {
                return file;
            }
            else if (targetType == byte[].class) {
                return file != null ? file.getContent() : null;
            }
            else if (targetType == String.class) {
                return file != null ? file.getFileName() : null;
            }
            else if (targetType == InputStream.class) {
                return file != null ? new ByteArrayInputStream(file.getContent()) : null;
            }
            
            // Par défaut, retourner l'objet UploadedFile
            return file;
        }

        // Sprint 10: Injection Map des fichiers
        // Détecter si c'est Map<String, UploadedFile> ou Map<String, Object>
        if (targetType == Map.class) {
            // Vérifier le type paramétré via reflection
            if (p.getParameterizedType() != null) {
                String typeStr = p.getParameterizedType().toString();
                
                // Si contient "UploadedFile" → Map de fichiers
                if (typeStr.contains("UploadedFile")) {
                    System.out.println("  -> Injection Map<String, UploadedFile>");
                    return new HashMap<>(uploadedFiles);
                }
            }
            
            // Sinon → Map de paramètres normaux
            System.out.println("  -> Injection Map<String, Object> (paramètres)");
            return new HashMap<>(paramMap);
        }

        // @Param
        if (p.isAnnotationPresent(Param.class)) {
            String key = p.getAnnotation(Param.class).value();
            return TypeConverter.convert(req.getParameter(key), targetType);
        }

        // @RequestParam
        if (p.isAnnotationPresent(RequestParam.class)) {
            String key = p.getAnnotation(RequestParam.class).value();
            return TypeConverter.convert(req.getParameter(key), targetType);
        }

        // Sprint 11: @SessionAttribute pour injecter une valeur de session
        if (p.isAnnotationPresent(SessionAttribute.class)) {
            SessionAttribute sa = p.getAnnotation(SessionAttribute.class);
            String key = sa.value();
            HttpSession session = req.getSession(false);
            
            Object value = (session != null) ? session.getAttribute(key) : null;
            
            if (value == null && sa.required()) {
                throw new UnauthorizedException("Session attribute '" + key + "' requis mais absent");
            }
            
            System.out.println("  -> @SessionAttribute('" + key + "') = " + value);
            return value;
        }

        // Sprint 11: Injection de MySession (wrapper session)
        if (targetType == MySession.class) {
            System.out.println("  -> Injection MySession");
            return new MySession(req.getSession(true));
        }

        // Sprint 11: Injection de HttpSession native
        if (targetType == HttpSession.class) {
            System.out.println("  -> Injection HttpSession");
            return req.getSession(true);
        }

        // Request / Response
        if (targetType == HttpServletRequest.class) return req;
        if (targetType == HttpServletResponse.class) return resp;

        // Auto-binding (Sprint 8 bis)
        if (ObjectBinder.isCustomClass(targetType)) {
            String prefix = detectPrefix(paramMap);
            return ObjectBinder.bindObject(targetType, paramMap, prefix);
        }

        // URL parameters
        String[] names = route.getPattern().getParamNames();
        if (names != null && index < names.length) {
            String raw = urlParams != null ? urlParams.get(names[index]) : null;
            return TypeConverter.convert(raw, targetType);
        }

        // GET/POST standard
        return TypeConverter.convert(req.getParameter(p.getName()), targetType);
    }


    // ===================================================================
    // HANDLE RETURN (SPRINT 9 MODERN JSON)
    // ===================================================================
    private void handleReturn(Object result, Method method,
                              HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        // ModelView ALWAYS wins → JSP rendering
        if (result instanceof ModelView mv) {
            for (Map.Entry<String, Object> e : mv.getData().entrySet())
                req.setAttribute(e.getKey(), e.getValue());

            req.getRequestDispatcher(mv.getView()).forward(req, resp);
            return;
        }

        // JSON mode?
        boolean isJson = method.isAnnotationPresent(Json.class);

        if (isJson) {

            JsonResponse json = buildJsonSuccessResponse(result);

            String payload = JsonSerializer.toJson(json);

            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(payload);

            System.out.println("  -> JSON envoyé : " + payload);
            return;
        }

        // Standard string
        if (result instanceof String s) {
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println(s);
            return;
        }

        // Default: serialize object raw
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().println(JsonSerializer.toJson(result));
    }


    // ===================================================================
    // JSON SUCCESS WRAPPER
    // ===================================================================
    private JsonResponse buildJsonSuccessResponse(Object result) {

        if (result == null)
            return new JsonResponse("success", 200, null, null);

        if (result instanceof Collection<?> col)
            return new JsonResponse("success", 200, col, col.size());

        if (result.getClass().isArray()) {

            int len = Array.getLength(result);
            List<Object> list = new ArrayList<>();

            for (int i = 0; i < len; i++)
                list.add(Array.get(result, i));

            return new JsonResponse("success", 200, list, list.size());
        }

        return new JsonResponse("success", 200, result, null);
    }


    // ===================================================================
    // UTILS
    // ===================================================================
    private String detectPrefix(Map<String, Object> map) {
        for (String key : map.keySet()) {
            if (key.contains("."))
                return key.substring(0, key.indexOf("."));
        }
        return null;
    }


    private void serveStaticFile(HttpServletResponse resp, InputStream resource, String path)
            throws IOException {

        String mime = getServletContext().getMimeType(path);
        if (mime == null) mime = "application/octet-stream";
        resp.setContentType(mime);

        try (OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = resource.read(buf)) != -1)
                out.write(buf, 0, len);
        }
    }


    // ===================================================================
    // Sprint 11 bis: VÉRIFICATION SÉCURITÉ
    // ===================================================================
    private void checkSecurity(Method method, HttpServletRequest req) 
            throws UnauthorizedException, ForbiddenException {
        
        // Si pas d'annotation @Auth → méthode publique, accès libre
        if (!method.isAnnotationPresent(Auth.class)) {
            return;
        }
        
        Auth auth = method.getAnnotation(Auth.class);
        String sessionKey = auth.sessionKey();    // Par défaut "user"
        String roleKey = auth.roleKey();          // Par défaut "userRole"
        String[] requiredRoles = auth.roles();    // Rôles autorisés
        
        HttpSession session = req.getSession(false);
        
        // ---------- Vérifier authentification ----------
        Object user = (session != null) ? session.getAttribute(sessionKey) : null;
        
        if (user == null) {
            System.out.println("  🔒 ACCÈS REFUSÉ: Utilisateur non connecté");
            throw new UnauthorizedException(
                "Authentification requise. Veuillez vous connecter."
            );
        }
        
        System.out.println("  🔓 Utilisateur connecté: " + user);
        
        // ---------- Vérifier rôle (si spécifié) ----------
        if (requiredRoles.length > 0) {
            Object userRole = session.getAttribute(roleKey);
            String userRoleStr = (userRole != null) ? userRole.toString() : "";
            
            boolean hasRole = false;
            for (String role : requiredRoles) {
                if (role.equalsIgnoreCase(userRoleStr)) {
                    hasRole = true;
                    break;
                }
            }
            
            if (!hasRole) {
                System.out.println("  🚫 ACCÈS REFUSÉ: Rôle insuffisant. Requis: " 
                    + Arrays.toString(requiredRoles) + ", Actuel: " + userRoleStr);
                throw new ForbiddenException(
                    Arrays.toString(requiredRoles), 
                    userRoleStr
                );
            }
            
            System.out.println("  ✅ Rôle vérifié: " + userRoleStr);
        }
    }


    // ===================================================================
    // 401 - Non authentifié
    // ===================================================================
    private void render401(HttpServletRequest req, HttpServletResponse resp,
                           String message) throws IOException {
        
        resp.setStatus(401);
        resp.setContentType("text/html;charset=UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>401 - Non autorisé</title>");
        out.println("<style>body{font-family:Arial;margin:50px;} .error{color:red;}</style></head><body>");
        out.println("<h1 class='error'>🔒 401 - Authentification requise</h1>");
        out.println("<p>" + message + "</p>");
        out.println("<a href='/login'>Se connecter</a>");
        out.println("</body></html>");
    }


    // ===================================================================
    // 403 - Accès interdit (mauvais rôle)
    // ===================================================================
    private void render403(HttpServletRequest req, HttpServletResponse resp,
                           String message) throws IOException {
        
        resp.setStatus(403);
        resp.setContentType("text/html;charset=UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>403 - Accès interdit</title>");
        out.println("<style>body{font-family:Arial;margin:50px;} .error{color:red;}</style></head><body>");
        out.println("<h1 class='error'>🚫 403 - Accès interdit</h1>");
        out.println("<p>" + message + "</p>");
        out.println("<a href='/'>Retour à l'accueil</a>");
        out.println("</body></html>");
    }


    // ===================================================================
    // 404
    // ===================================================================
    private void render404(HttpServletRequest req, HttpServletResponse resp,
                           String path, String httpMethod) throws IOException {

        resp.setStatus(404);
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("<h1>404 - Route non trouvée</h1>");
        out.println("<p>Méthode : " + httpMethod + "</p>");
        out.println("<p>URL : " + path + "</p>");
    }
}
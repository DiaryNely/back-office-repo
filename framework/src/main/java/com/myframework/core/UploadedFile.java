package com.myframework.core;

import java.io.*;

/**
 * Sprint 10 : Représente un fichier uploadé via multipart/form-data
 */
public class UploadedFile {
    
    private String fieldName;      // Nom du champ dans le formulaire
    private String fileName;       // Nom du fichier original
    private String contentType;    // Type MIME (image/png, application/pdf, etc.)
    private long size;             // Taille en bytes
    private byte[] content;        // Contenu du fichier
    
    // Constructeur
    public UploadedFile(String fieldName, String fileName, String contentType, 
                        long size, byte[] content) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.content = content;
    }
    
    // Getters
    public String getFieldName() {
        return fieldName;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public long getSize() {
        return size;
    }
    
    public byte[] getContent() {
        return content;
    }
    
    /**
     * Obtenir l'extension du fichier
     */
    public String getExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * Sauvegarder le fichier sur le disque
     */
    public void saveTo(String targetPath) throws IOException {
        File file = new File(targetPath);
        
        // Créer les répertoires parents si nécessaire
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
    }
    
    /**
     * Sauvegarder dans un répertoire avec le nom original
     */
    public void saveToDirectory(String directoryPath) throws IOException {
        String fullPath = directoryPath + File.separator + fileName;
        saveTo(fullPath);
    }
    
    /**
     * Obtenir le contenu sous forme de String (pour fichiers texte)
     */
    public String getContentAsString() {
        return new String(content);
    }
    
    /**
     * Vérifier si c'est une image
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Vérifier si c'est un PDF
     */
    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }
    
    /**
     * Obtenir une taille formatée lisible
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public String toString() {
        return "UploadedFile{" +
                "fieldName='" + fieldName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + getFormattedSize() +
                '}';
    }
}
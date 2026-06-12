package made.archive.service.document;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface StorageService
{
    /**
     * Upload un fichier et retourne la clé de stockage
     */
    String upload(MultipartFile file, String typeDocument);

    /**
     * Upload des bytes bruts (texte OCR, fichiers convertis...)
     */
    String uploadBytes(byte[] bytes, String key, String contentType);

    /**
     * Télécharge le contenu brut (pour vérification SHA-256, OCR...)
     */
    InputStream download(String key);

    /**
     * Supprime un fichier
     */
    void delete(String key);

    /**
     * Upload le fichier original (avant conversion PDF/A-3b)
     * Clé : original/{typeDocument}/{date}/{uuid}/{filename}
     */
    String uploadOriginal(MultipartFile file, String typeDocument);
}
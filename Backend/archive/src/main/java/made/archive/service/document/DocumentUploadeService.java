package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.entite.*;
import made.archive.exception.BusinessException;
import made.archive.exception.PdfAConversionException;
import made.archive.repository.DocumentRepository;
import made.archive.repository.GroupeAccessRepository;
import made.archive.repository.TypeDocumentRepository;
import made.archive.repository.UserRepository;
import made.archive.dto.DocumentUploadDto;
import made.archive.dto.DocumentUploadResultDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadeService
{
    private final StorageService storageService;
    private final HashService hashService;
    private final LibreOfficeConversionService libreOfficeConversionService;
    private final PdfAConversionService pdfAConversionService;
    private final OcrService ocrService;
    private final MeilisearchService meilisearchService;
    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final UserRepository userRepository;
    private final GroupeAccessRepository groupeAccessRepository;

    @Transactional
    public DocumentUploadResultDto upload(MultipartFile file, DocumentUploadDto dto)
    {
        // 1. Validation des entités liées
        TypeDocument typeDocument = typeDocumentRepository.findById(dto.getTypeDocumentId())
            .orElseThrow(() -> new BusinessException(
                "Type de document introuvable : " + dto.getTypeDocumentId()));
    
        User uploadedBy = userRepository.findById(dto.getUploadedById())
            .orElseThrow(() -> new BusinessException(
                "Utilisateur introuvable : " + dto.getUploadedById()));
    
        // 2. Validation accès privé
        if (dto.getAccess() == TypeAccess.PRIVE &&
            (dto.getGroupeNom() == null || dto.getGroupeNom().isBlank()))
        {
            throw new BusinessException(
                "Le nom du groupe est obligatoire pour un document privé");
        }
    
        try
        {
            String originalFilename = file.getOriginalFilename();
            byte[] originalBytes = file.getBytes();
    
            // 3. Upload du fichier original
            String originalStorageKey = storageService.uploadOriginal(
                file, typeDocument.getNom());
    
            // 4. Conversion en PDF via LibreOffice
            byte[] pdfBytes = libreOfficeConversionService.convertToPdf(
                originalBytes, originalFilename);
    
            // 5. Marquage PDF/A-3b via PDFBox
            byte[] pdfABytes = pdfAConversionService.convertToPdfA3(pdfBytes);
    
            // 6. Calcul SHA-256 sur le PDF/A-3b final
            String sha256 = hashService.calculateFromBytes(pdfABytes);
    
            // 7. Vérification unicité du hash
            if (documentRepository.existsBySha256Hash(sha256))
            {
                throw new BusinessException(
                    "Ce document existe déjà en archive (hash identique)");
            }
    
            // 8. Stockage du PDF/A-3b
            String pdfAStorageKey = storageService.uploadBytes(
                pdfABytes,
                "pdfa/" + typeDocument.getNom() + "/" + LocalDate.now() + "/"
                + UUID.randomUUID() + "/" + originalFilename + ".pdf",
                "application/pdf"
            );
    
            // 9. Calcul date de rétention
            LocalDate retentionUntil = LocalDate.now().plusYears(
                typeDocument.getRetention().getRetentionYears());
    
            // 10. Création du Document
            Document document = new Document();
            document.setTitre(originalFilename);
            document.setAccess(dto.getAccess());
            document.setSha256Hash(sha256);
            document.setStorageKey(pdfAStorageKey);
            document.setOriginalStorageKey(originalStorageKey);
            document.setRetentionUntil(retentionUntil);
            document.setCreateAt(LocalDateTime.now());
            document.setVersion(1L);
            document.setStatus(DocumentStatus.PENDING);
            document.setIntegrityLevel(dto.getIntegrityLevel());
            document.setTypeDocument(typeDocument);
            document.setUploadedBy(uploadedBy);
    
            // 11. Création du groupe si accès privé
            if (dto.getAccess() == TypeAccess.PRIVE)
            {
                GroupeAccess groupe = new GroupeAccess();
                groupe.setNom(dto.getGroupeNom());
                groupe.setCreateAt(LocalDate.now());
    
                // L'uploadeur est toujours membre
                List<User> membres = new ArrayList<>();
                membres.add(uploadedBy);
    
                // Ajout des autres membres si fournis
                if (dto.getGroupeMembresIds() != null &&
                    !dto.getGroupeMembresIds().isEmpty())
                {
                    List<User> autresMembres = userRepository.findAllById(
                        dto.getGroupeMembresIds().stream()
                            .filter(id -> !id.equals(uploadedBy.getId()))
                            .toList()
                    );
                    membres.addAll(autresMembres);
                }
    
                groupe.setMembres(membres);
                document.setGroupe(groupeAccessRepository.save(groupe));
            }
    
            // 12. Sauvegarde du Document
            document = documentRepository.save(document);
            log.info("[Upload] Document sauvegardé : {}", document.getId());
    
            // 13. OCR — retourne le texte extrait pour le pré-remplissage
            log.info("[Upload] OCR en cours...");
            String extractedText = ocrService.processDocument(document);
    
            // 14. Indexation Meilisearch
            if (extractedText != null && !extractedText.isBlank())
            {
                log.info("[Upload] Indexation Meilisearch...");
                meilisearchService.indexDocument(document, extractedText);
            }
    
            // 15. Passage en ACTIVE
            document.setStatus(DocumentStatus.ACTIVE);
            documentRepository.save(document);
            log.info("[Upload] Document ACTIVE : {}", document.getId());
    
            // 16. Construction du résultat avec suggestions de pré-remplissage
            return buildResult(document, extractedText, typeDocument);
        }
        catch (BusinessException e)
        {
            throw e;
        }
        catch (PdfAConversionException e)
        {
            throw new BusinessException("Échec conversion PDF/A-3 : " + e.getMessage());
        }
        catch (Exception e)
        {
            throw new BusinessException(
                "Erreur lors de l'upload : " + e.getMessage(), e);
        }
    }

    /**
     * Construit le résultat retourné au frontend avec les suggestions
     * de pré-remplissage des métadonnées via regex.
     */
    private DocumentUploadResultDto buildResult(Document document,
                                                 String extractedText,
                                                 TypeDocument typeDocument)
    {
        DocumentUploadResultDto result = new DocumentUploadResultDto();
        result.setDocumentId(document.getId());
        result.setStatus(document.getStatus());
        result.setSha256Hash(document.getSha256Hash());
        result.setStorageKey(document.getStorageKey());
        result.setOriginalStorageKey(document.getOriginalStorageKey());

        // Pré-remplissage via regex si texte disponible
        if (extractedText != null && !extractedText.isBlank())
        {
            java.util.Map<String, String> suggestions = new java.util.LinkedHashMap<>();
            for (made.archive.entite.MetaData metaData : typeDocument.getMetaData())
            {
                if (metaData.getExtractionRegex() == null) continue;
                try
                {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern
                        .compile(metaData.getExtractionRegex());
                    java.util.regex.Matcher matcher = pattern.matcher(extractedText);
                    if (matcher.find())
                    {
                        suggestions.put(metaData.getNom(), matcher.group());
                    }
                }
                catch (Exception e)
                {
                    log.warn("[Upload] Regex invalide pour '{}' : {}",
                             metaData.getNom(), metaData.getExtractionRegex());
                }
            }
            result.setMetaDataSuggestions(suggestions);
        }

        return result;
    }
}
package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.entite.CheckResult;
import made.archive.entite.Document;
import made.archive.entite.DocumentStatus;
import made.archive.entite.FixityCheckResult;
import made.archive.repository.DocumentRepository;
import made.archive.repository.FixityCheckResultRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixityCheckService 
{
    private final StorageService storageService;
    private final HashService hashService;
    private final DocumentRepository documentRepository;
    private final FixityCheckResultRepository fixityCheckResultRepository;

    /**
     * Vérifie l'intégrité d'une liste de documents par leurs IDs.
     */
    @Transactional
    public List<Document> verifyDocumentsByIds(List<UUID> documentIds) 
    {
        try
        {
            List<Document> documents = documentRepository.findAllById(documentIds);
        return documents.stream()
            .peek(this::verifyAndSave)
            .collect(Collectors.toList());
        }
        catch(Exception e)
        {
            throw new RuntimeException("Erreur lors verification des integrites des documents", e);
        }
    }

    /**
     * Vérifie l'intégrité de TOUS les documents d'un type spécifique.
     */
    @Transactional
    public List<Document> verifyDocumentsByType(Long typeDocumentId) 
    {
        try
        {
            List<Document> documents = documentRepository.findByTypeDocument_Id(typeDocumentId);
            return documents.stream()
                .peek(this::verifyAndSave)
                .collect(Collectors.toList());
        }
        catch(Exception e)
        {
            throw new RuntimeException("Erreur lors verification des integrites des documents", e);
        }
    }

    /**
     * Vérifie TOUS les documents de la plateforme.
     */
    @Transactional
    public List<Document> verifyAllDocuments() 
    {
        try
        {
            List<Document> documents = documentRepository.findAll();
            return documents.stream()
                .peek(this::verifyAndSave)
                .collect(Collectors.toList());
        }
        catch(Exception e)
        {
            throw new RuntimeException("Erreur lors verification des integrites des documents", e);
        }
    }

    /**
     * Vérifie un document individuel et enregistre le résultat.
     */
    private void verifyAndSave(Document document) 
    {
        if (document.getStatus() == DocumentStatus.CORRUPTED || 
        document.getStatus() == DocumentStatus.DELETED) 
        {
            return;
        }

        InputStream fileStream;
        try 
        {
            fileStream = storageService.download(document.getStorageKey());
        } 
        catch (Exception e) 
        {
            log.warn("Impossible de télécharger {} : {}", document.getStorageKey(), e.getMessage());
            return;
        }

        if (fileStream == null) 
        {
            saveResult(document, CheckResult.EMPTY);
            return;
        }

        try 
        {
            String actualHash = hashService.calculateFromStream(fileStream);
            if (actualHash.equals(document.getSha256Hash())) 
            {
                saveResult(document, CheckResult.OK);
            } 
            else 
            {
                saveResult(document, CheckResult.CORRUPTED);
            }
        } 
        catch (Exception e) 
        {
            log.error("Erreur calcul hash pour {} : {}", document.getId(), e.getMessage());
        }
    }

    /**
     * Enregistre le résultat en base de données.
     */
    @Transactional
    private void saveResult(Document document, CheckResult result) 
    {
        try
        {
            Optional<FixityCheckResult> existing = 
                fixityCheckResultRepository.findByDocumentId(document.getId());

            FixityCheckResult fixityCheckResult;
            
            if (existing.isPresent()) 
            {
                fixityCheckResult = existing.get();
            } 
            else 
            {
                fixityCheckResult = new FixityCheckResult();
                fixityCheckResult.setDocument(document);
            }

            fixityCheckResult.setCheckedAt(LocalDate.now());
            fixityCheckResult.setResult(result);
            fixityCheckResultRepository.save(fixityCheckResult);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Erreur lors de l'enregistrement du resultat", e);
        }
    }
}
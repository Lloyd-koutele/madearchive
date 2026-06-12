package made.archive.service.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.IndexesQuery;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.MeilisearchProperties;
import made.archive.dto.MeilisearchDocumentDto;
import made.archive.entite.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeilisearchService
{
    private final Client meilisearchClient;
    private final MeilisearchProperties meilisearchProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String INDEX_NAME = "documents";

    @PostConstruct
    public void init()
    {
        try
        {
            fetchAndStoreSearchKey();
            getOrCreateIndex();
            log.info("[Meilisearch] Index '{}' prêt", INDEX_NAME);
        }
        catch (Exception e)
        {
            log.error("[Meilisearch] Échec initialisation : {}", e.getMessage());
        }
    }

    private void fetchAndStoreSearchKey()
    {
        try
        {
            WebClient client = webClientBuilder
                .baseUrl(meilisearchProperties.getHost())
                .defaultHeader("Authorization",
                    "Bearer " + meilisearchProperties.getApiKey())
                .build();

            String response = client.get()
                .uri("/keys")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> json = objectMapper.readValue(
                response, new TypeReference<Map<String, Object>>(){});

            List<Map<String, Object>> results = objectMapper.convertValue(
                json.get("results"),
                new TypeReference<List<Map<String, Object>>>(){});

            String searchKey = results.stream()
                .filter(k -> "Default Search API Key".equals(k.get("name")))
                .map(k -> (String) k.get("key"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Default Search API Key introuvable"));

            meilisearchProperties.setSearchKey(searchKey);
            log.info("[Meilisearch] Search API Key récupérée automatiquement");
        }
        catch (Exception e)
        {
            log.warn("[Meilisearch] Impossible de récupérer la Search Key : {}",
                     e.getMessage());
        }
    }

    public void indexDocument(Document document, String extractedText)
    {
        try
        {
            Index index = getOrCreateIndex();

            MeilisearchDocumentDto dto = MeilisearchDocumentDto.builder()
                .id(document.getId().toString())
                .titre(document.getTitre())
                .typeDocument(document.getTypeDocument().getNom())
                .typeDocumentId(document.getTypeDocument().getId())
                .extractedText(extractedText)
                .status(document.getStatus().name())
                .access(document.getAccess().name())
                .retentionUntil(document.getRetentionUntil())
                .uploadedBy(document.getUploadedBy().getId().toString())
                .groupeId(document.getGroupe() != null
                    ? document.getGroupe().getId().toString() : null)
                .build();

            String json = objectMapper.writeValueAsString(List.of(dto));
            index.addDocuments(json);
            log.info("[Meilisearch] Document indexé : {}", document.getId());
        }
        catch (Exception e)
        {
            log.error("[Meilisearch] Échec indexation document {} : {}",
                      document.getId(), e.getMessage());
        }
    }

    public void updateDocumentStatus(Document document)
    {
        try
        {
            Index index = getOrCreateIndex();
            String partialUpdate = "[{\"id\":\"" + document.getId() +
                                   "\",\"status\":\"" + document.getStatus().name() + "\"}]";
            index.updateDocuments(partialUpdate);
            log.info("[Meilisearch] Statut mis à jour : {} → {}",
                     document.getId(), document.getStatus());
        }
        catch (Exception e)
        {
            log.error("[Meilisearch] Échec mise à jour statut {} : {}",
                      document.getId(), e.getMessage());
        }
    }

    public void deleteDocument(String documentId)
    {
        try
        {
            Index index = getOrCreateIndex();
            index.deleteDocument(documentId);
            log.info("[Meilisearch] Document supprimé de l'index : {}", documentId);
        }
        catch (Exception e)
        {
            log.error("[Meilisearch] Échec suppression index {} : {}",
                      documentId, e.getMessage());
        }
    }

    private Index getOrCreateIndex() throws MeilisearchException
    {
        IndexesQuery query = new IndexesQuery().setLimit(100);
        boolean exists = Arrays.stream(
            meilisearchClient.getIndexes(query).getResults())
            .anyMatch(i -> INDEX_NAME.equals(i.getUid()));

        if (!exists)
        {
            log.info("[Meilisearch] Création de l'index '{}'", INDEX_NAME);
            meilisearchClient.createIndex(INDEX_NAME, "id");
            Index index = meilisearchClient.getIndex(INDEX_NAME);
            index.updateFilterableAttributesSettings(new String[]{
                "typeDocument", "typeDocumentId", "status",
                "access", "uploadedBy", "groupeId"
            });
            return index;
        }

        return meilisearchClient.getIndex(INDEX_NAME);
    }
}
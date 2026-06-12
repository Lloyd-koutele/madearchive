package made.archive.service.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.MeilisearchProperties;
import made.archive.dto.SearchRequestDto;
import made.archive.dto.SearchResultDto;
import made.archive.dto.SearchResultItemDto;
import made.archive.entite.TypeAccess;
import made.archive.entite.User;
import made.archive.exception.BusinessException;
import made.archive.repository.DocumentRepository;
import made.archive.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchService
{
    private final WebClient.Builder webClientBuilder;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MeilisearchProperties meilisearchProperties;
    private final ObjectMapper objectMapper;

    private static final String INDEX_NAME = "documents";

    public SearchResultDto search(SearchRequestDto request, UserDetails userDetails)
    {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        try
        {
            // Construire le body de la requête manuellement
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("q", request.getQuery());
            body.put("page", request.getPage());
            body.put("hitsPerPage", request.getHitsPerPage());
            body.put("attributesToRetrieve", List.of(
                "id", "titre", "typeDocument",
                "access", "status", "retentionUntil",
                "uploadedBy", "groupeId"
            ));

            // Filtres
            List<String> filters = new ArrayList<>();
            if (request.getTypeDocumentId() != null)
            {
                filters.add("typeDocumentId = " + request.getTypeDocumentId());
            }
            filters.add("status != DELETED");
            body.put("filter", filters);

            // Appel direct à l'API Meilisearch via WebClient
            WebClient client = webClientBuilder
                .baseUrl(meilisearchProperties.getHost())
                .defaultHeader("Authorization",
                    "Bearer " + meilisearchProperties.getSearchKey())
                .build();

            String responseJson = client.post()
                .uri("/indexes/" + INDEX_NAME + "/search")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Parser la réponse avec Jackson
            Map<String, Object> response = objectMapper.readValue(
                responseJson,
                new TypeReference<Map<String, Object>>(){});

            List<Map<String, Object>> hits = objectMapper.convertValue(
                response.get("hits"),
                new TypeReference<List<Map<String, Object>>>(){});

            int totalHits = (int) response.getOrDefault("totalHits", 0);
            int totalPages = (int) response.getOrDefault("totalPages", 1);

            if (hits == null) hits = Collections.emptyList();

            // Filtrer selon les droits d'accès
            List<Map<String, Object>> filtered = hits.stream()
                .filter(hit -> isAccessible(hit, user))
                .collect(Collectors.toList());

            List<SearchResultItemDto> items = filtered.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

            log.info("[Search] '{}' → {} résultats pour {}",
                     request.getQuery(), items.size(), user.getEmail());

            return SearchResultDto.builder()
                .totalHits(totalHits)
                .page(request.getPage())
                .hitsPerPage(request.getHitsPerPage())
                .totalPages(totalPages)
                .results(items)
                .build();
        }
        catch (Exception e)
        {
            // Index inexistant → aucun document encore indexé
            if (e.getMessage() != null && e.getMessage().contains("404"))
            {
                log.warn("[Search] Index '{}' inexistant — aucun document indexé", INDEX_NAME);
                return SearchResultDto.builder()
                    .totalHits(0)
                    .page(request.getPage())
                    .hitsPerPage(request.getHitsPerPage())
                    .totalPages(0)
                    .results(Collections.emptyList())
                    .build();
            }
            log.error("[Search] Erreur recherche : {}", e.getMessage());
            throw new BusinessException("Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private boolean isAccessible(Map<String, Object> hit, User user)
    {
        String access = (String) hit.get("access");

        if (TypeAccess.PUBLIC.name().equals(access))
        {
            return true;
        }

        if (TypeAccess.PRIVE.name().equals(access))
        {
            String documentIdStr = (String) hit.get("id");
            try
            {
                UUID documentId = UUID.fromString(documentIdStr);
                return documentRepository.findById(documentId)
                    .map(doc -> doc.getGroupe() != null &&
                        doc.getGroupe().getMembres().stream()
                            .anyMatch(m -> m.getId().equals(user.getId())))
                    .orElse(false);
            }
            catch (Exception e)
            {
                return false;
            }
        }

        return false;
    }

    private SearchResultItemDto toDto(Map<String, Object> hit)
    {
        String retentionStr = (String) hit.get("retentionUntil");
        LocalDate retentionUntil = retentionStr != null
            ? LocalDate.parse(retentionStr) : null;

        return SearchResultItemDto.builder()
            .documentId(UUID.fromString((String) hit.get("id")))
            .titre((String) hit.get("titre"))
            .typeDocument((String) hit.get("typeDocument"))
            .access((String) hit.get("access"))
            .status((String) hit.get("status"))
            .retentionUntil(retentionUntil)
            .build();
    }
}
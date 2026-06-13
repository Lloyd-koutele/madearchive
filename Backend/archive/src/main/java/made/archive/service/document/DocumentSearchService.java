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
import org.springframework.http.MediaType;
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
            // ── 1. Corps de la requête Meilisearch ──────────────────────────
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("q", request.getQuery() != null ? request.getQuery() : "");
            body.put("page", request.getPage());
            body.put("hitsPerPage", request.getHitsPerPage());
            body.put("attributesToRetrieve", List.of(
                "id", "titre", "typeDocument",
                "access", "status", "retentionUntil",
                "uploadedBy", "groupeId"
            ));

            // ── 2. Filtres — string unique avec AND ──────────────────────────
            List<String> filterParts = new ArrayList<>();
            if (request.getTypeDocumentId() != null)
            {
                filterParts.add("typeDocumentId = " + request.getTypeDocumentId());
            }
            filterParts.add("status != DELETED");
            body.put("filter", String.join(" AND ", filterParts));

            // ── 3. Appel WebClient vers Meilisearch ──────────────────────────
            WebClient client = webClientBuilder
                .baseUrl(meilisearchProperties.getHost())
                .defaultHeader("Authorization",
                    "Bearer " + meilisearchProperties.getSearchKey())
                .build();

            String responseJson = client.post()
                .uri("/indexes/" + INDEX_NAME + "/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // ── 4. Parse de la réponse ───────────────────────────────────────
            Map<String, Object> response = objectMapper.readValue(
                responseJson,
                new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hits = response.get("hits") instanceof List<?>
                ? (List<Map<String, Object>>) response.get("hits")
                : Collections.emptyList();

            int totalHits  = toInt(response.get("totalHits"), 0);
            int totalPages = toInt(response.get("totalPages"), 1);

            // ── 5. Filtre par droits d'accès ─────────────────────────────────
            List<Map<String, Object>> accessible = hits.stream()
                .filter(hit -> isAccessible(hit, user))
                .collect(Collectors.toList());

            List<SearchResultItemDto> items = accessible.stream()
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

    // ── Accessibilité ────────────────────────────────────────────────────────

    private boolean isAccessible(Map<String, Object> hit, User user)
    {
        String access = asString(hit.get("access"));

        if (TypeAccess.PUBLIC.name().equals(access))
        {
            return true;
        }

        if (TypeAccess.PRIVE.name().equals(access))
        {
            String idStr = asString(hit.get("id"));
            if (idStr == null) return false;
            try
            {
                UUID documentId = UUID.fromString(idStr);
                return documentRepository.findById(documentId)
                    .map(doc -> doc.getGroupe() != null &&
                        doc.getGroupe().getMembres().stream()
                            .anyMatch(m -> m.getId().equals(user.getId())))
                    .orElse(false);
            }
            catch (Exception e)
            {
                log.warn("[Search] UUID invalide dans le hit : {}", idStr);
                return false;
            }
        }

        return false;
    }

    // ── Conversion hit → DTO ─────────────────────────────────────────────────

    private SearchResultItemDto toDto(Map<String, Object> hit)
    {
        String idStr        = asString(hit.get("id"));
        String retentionStr = asString(hit.get("retentionUntil"));

        UUID documentId = null;
        if (idStr != null)
        {
            try 
            {
                documentId = UUID.fromString(idStr); 
            }
            catch (IllegalArgumentException e) 
            {
                log.warn("[Search] UUID invalide : {}", idStr);
            }
        }

        LocalDate retentionUntil = null;
        if (retentionStr != null)
        {
            try 
            { 
                retentionUntil = LocalDate.parse(retentionStr); 
            }
            catch (Exception e) 
            {
                log.warn("[Search] Date invalide : {}", retentionStr);
            }
        }

        return SearchResultItemDto.builder()
            .documentId(documentId)
            .titre(asString(hit.get("titre")))
            .typeDocument(asString(hit.get("typeDocument")))
            .access(asString(hit.get("access")))
            .status(asString(hit.get("status")))
            .retentionUntil(retentionUntil)
            .build();
    }

    // ── Utilitaires de cast sûrs ─────────────────────────────────────────────

    /**
     * Convertit un Object en String de manière sûre.
     * Retourne null si la valeur est null ou n'est pas une String.
     */
    private String asString(Object value)
    {
        if (value instanceof String s) return s;
        if (value != null) return value.toString();
        return null;
    }

    /**
     * Convertit un Object en int avec une valeur par défaut.
     */
    private int toInt(Object value, int defaultValue)
    {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s)
        {
            try 
            {
                return Integer.parseInt(s); 
            }
            catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
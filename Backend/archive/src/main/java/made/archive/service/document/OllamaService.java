package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.OllamaProperties;
import made.archive.entite.MetaData;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService 
{

    private final OllamaProperties ollamaProperties;
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    private static final String MODEL = "qwen2.5-coder:3b";
    private static final int MAX_TEXT_CHARS = 3000;

    @PostConstruct
    public void init() 
    {
        this.webClient = webClientBuilder.baseUrl(ollamaProperties.getBaseUrl()).build();
    }

    /**
     * Génère les Regex en parallèle pour toute la liste de métadonnées.
     */
    public void generateRegexForMetaData(List<MetaData> metaDataList, String ocrText) 
    {
        String truncatedText = (ocrText != null && ocrText.length() > MAX_TEXT_CHARS)
                ? ocrText.substring(0, MAX_TEXT_CHARS)
                : ocrText;

        // On filtre et on traite de manière réactive
        Flux.fromIterable(metaDataList)
                .filter(metaData -> metaData.getExtractionRegex() == null)
                .flatMap(metaData -> callQwenAsync(metaData.getNom(), metaData.getMetaDataType().name(), truncatedText)
                        .doOnNext(regex -> {
                            metaData.setExtractionRegex(regex);
                            log.info("[Ollama] Regex générée pour '{}' : {}", metaData.getNom(), regex);
                        })
                        .onErrorResume(e -> {
                            log.warn("[Ollama] Échec génération regex pour '{}' : {}", metaData.getNom(), e.getMessage());
                            return Mono.empty(); // On ignore l'erreur pour continuer le Flux
                        })
                )
                // blockLast() attend que TOUT le flux parallèle soit terminé de manière synchrone pour la méthode appelante
                .blockLast(); 
    }

    private Mono<String> callQwenAsync(String fieldName, String fieldType, String ocrText) 
    {
        String prompt = buildPrompt(fieldName, fieldType, ocrText);

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", false
        );

        return this.webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (response != null && response.containsKey("response")) 
                    {
                        String rawRegex = (String) response.get("response");
                        return cleanRegex(rawRegex);
                    }
                    throw new RuntimeException("Réponse Ollama vide ou invalide");
                });
    }

    private String buildPrompt(String fieldName, String fieldType, String ocrText) 
    {
        return String.format("""
            You are a regex expert. Analyze the following document text and generate \
            a single Java regex pattern to extract the field described below.
            
            Field name: %s
            Field type: %s
            
            Document text (extract):
            ---
            %s
            ---
            
            Rules:
            - Return ONLY the raw regex pattern, nothing else.
            - Do NOT wrap the response in markdown code blocks like ```regex ... ```. Just return the raw string.
            - No explanation, no quotes.
            - The regex must work with Java Pattern.compile().
            - If you cannot determine a pattern, return: .+
            
            Regex:
            """, fieldName, fieldType, ocrText);
    }

    private String cleanRegex(String raw) 
    {
        if (raw == null) return null;

        String cleaned = raw.trim();
        
        // Sécurité si Qwen utilise quand même des blocs de code markdown (ex: ```regex ... ```)
        if (cleaned.startsWith("```")) 
        {
            // On retire la ligne d'ouverture (ex: ```regex ou ```) et la ligne de fermeture
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n", "")
                             .replaceAll("\\n```$", "");
        }
        
        // Supprime les reliquats de backticks simples et nettoie les lignes
        return cleaned.replaceAll("`", "")
                      .replaceAll("(?m)^\\s*Regex:\\s*", "")
                      .trim()
                      .lines()
                      .findFirst()
                      .orElse(null);
    }
}
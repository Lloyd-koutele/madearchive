package made.archive.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MeilisearchDocumentDto
{
    // Champ id obligatoire pour Meilisearch
    private String id;
    private String titre;
    private String typeDocument;
    private String extractedText;
    private String status;
    private String access;
    private LocalDate retentionUntil;
    private String uploadedBy;
    private Long typeDocumentId; 
    private String groupeId;     
}
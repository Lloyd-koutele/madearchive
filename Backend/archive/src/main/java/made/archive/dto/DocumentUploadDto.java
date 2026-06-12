package made.archive.dto;

import lombok.Data;
import made.archive.entite.IntegrityLevel;
import made.archive.entite.TypeAccess;

import java.util.List;
import java.util.UUID;

@Data
public class DocumentUploadDto
{
    private String titre;
    private TypeAccess access;
    private Long typeDocumentId;
    private UUID uploadedById;
    private IntegrityLevel integrityLevel;

    // Obligatoire si access == PRIVE
    private String groupeNom;

    // Optionnel — l'uploadeur est toujours ajouté automatiquement
    private List<UUID> groupeMembresIds;
}
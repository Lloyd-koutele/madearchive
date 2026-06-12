package made.archive.dto;

import lombok.Data;
import made.archive.entite.DocumentStatus;

import java.util.Map;
import java.util.UUID;

@Data
public class DocumentUploadResultDto
{
    private UUID documentId;
    private DocumentStatus status;
    private String sha256Hash;
    private String storageKey;
    private String originalStorageKey;
    private Map<String, String> metaDataSuggestions;
}
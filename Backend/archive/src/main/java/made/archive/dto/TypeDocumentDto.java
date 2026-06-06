package made.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypeDocumentDto 
{
    private Long id;
    private String nom;
    private List<MetaDataDto> metaData;
    private UUID userId;
    private Long retentionYears;
    private Long periodGrace;   
}

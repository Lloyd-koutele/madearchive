package made.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import made.archive.entite.MetaDataType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaDataDto 
{
    private Long id;
    private String nom;
    private Boolean obligatoire;
    private MetaDataType metaDataType;
}

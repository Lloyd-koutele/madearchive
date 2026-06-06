package made.archive.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="meta_datas")
@Entity
public class MetaData 
{
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank(message = "La signature hash est obligatoire")
    @Column(nullable = false, length = 100)
    private String nom;

    @NotNull
    @Column(nullable = false)
    private Boolean obligatoire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetaDataType metaDataType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;
}

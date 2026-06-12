package made.archive.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @NotBlank(message = "Le nom de la métadonnée est obligatoire")
    @Column(nullable = false, length = 100)
    private String nom;

    @NotNull
    @Column(nullable = false)
    private Boolean obligatoire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetaDataType metaDataType;

    // Regex générée par Qwen au premier document — nullable jusqu'alors
    @Column(length = 500)
    private String extractionRegex;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;
}
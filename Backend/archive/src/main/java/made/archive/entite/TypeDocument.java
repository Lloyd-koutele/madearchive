package made.archive.entite;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.CascadeType;

import java.util.List;

 

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="type_documents")
@Entity
public class TypeDocument 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La signature hash est obligatoire")
    @Column(nullable = false, length = 100)
    private String nom;

    @NotNull
    @JoinColumn(name = "retention_id", nullable = false)
    @ManyToOne(cascade = CascadeType.ALL)
    private Retention retention;

    @OneToMany(mappedBy = "typeDocument", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @ToString.Exclude
    private List<MetaData> metaData;

    @OneToMany(mappedBy = "typeDocument", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Document> documents;

    @NotNull
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    private User user;
}

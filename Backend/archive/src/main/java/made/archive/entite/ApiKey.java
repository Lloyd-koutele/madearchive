package made.archive.entite;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Table(name="apikeys")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class ApiKey 
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "L'api key est obligatoire")
    @Column(nullable = false, length = 100, unique = true)
    private String prefixe;

    @NotBlank(message = "L'api key est obligatoire")
    @Column(nullable = false, length = 100, unique = true)
    private String keyHash;

    @NotBlank(message = "Le nom de la key est obligatoire")
    @Column(nullable = false, length = 100)
    private String nom;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;

    @Column(nullable = false, length = 20)
    private LocalDate createdAt;

    @Column(nullable = false, length = 20)
    private LocalDate expiresAt;

    @Column(nullable = false, length = 20)
    private Boolean actif;
}

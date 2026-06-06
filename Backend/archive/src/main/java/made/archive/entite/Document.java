package made.archive.entite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import  java.util.UUID;
import java.util.List;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import lombok.Data;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="documents")
@Entity
public  class Document
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false, length = 50)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeAccess access;

    @NotBlank(message = "La signature hash est obligatoire")
    @Column(nullable = false, length = 100)
    private String sha256Hash;

    @Column(length = 100)
    private String blockChainTxld;

    @NotNull
    private LocalDate retentionUntil;

    @NotNull
    private LocalDateTime createAt;

    @NotNull
    private Long version;

    @NotBlank(message = "La signature hash est obligatoire")
    @Column(nullable = false, length = 100)
    private String minIOkey;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<DataType> data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntegretyLevel integretyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_id")
    private GroupeAccess groupe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;
}
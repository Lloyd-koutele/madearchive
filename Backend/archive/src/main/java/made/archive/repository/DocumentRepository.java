package made.archive.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import made.archive.entite.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> 
{
    Optional<Document> findDocumntByTitre(String titre);
    List<Document> findByTypeDocument_Id(Long id);
    boolean existsBySha256Hash(String sha256Hash);
}

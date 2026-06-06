package made.archive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import made.archive.entite.TypeDocument;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TypeDocumentRepository extends JpaRepository<TypeDocument, Long>
{
    Optional<TypeDocument> findByNom(String nom);

    @Query("SELECT t FROM TypeDocument t WHERE t.user.id = :userId")
    List<TypeDocument> findByTypeDocumentCreateByUserId(@Param("userId") UUID userId);

    boolean existsByDocumentsNotEmptyAndId(Long id);
}

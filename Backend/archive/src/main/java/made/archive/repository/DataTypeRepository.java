package made.archive.repository;

import made.archive.entite.DataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DataTypeRepository extends JpaRepository<DataType, Long>
{
    @Modifying
    @Query("DELETE FROM DataType d WHERE d.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}
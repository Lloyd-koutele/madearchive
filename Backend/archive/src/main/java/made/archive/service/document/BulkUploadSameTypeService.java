package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.dto.BulkUploadItemResultDto;
import made.archive.dto.BulkUploadReportDto;
import made.archive.dto.DocumentUploadDto;
import made.archive.dto.DocumentUploadResultDto;
import made.archive.entite.TypeDocument;
import made.archive.exception.BusinessException;
import made.archive.repository.TypeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadSameTypeService
{
    private final DocumentUploadeService documentUploadService;
    private final TypeDocumentRepository typeDocumentRepository;

    public BulkUploadReportDto uploadAll(List<MultipartFile> files,
                                          Long typeDocumentId,
                                          java.util.UUID uploadedById)
    {
        TypeDocument typeDocument = typeDocumentRepository.findById(typeDocumentId)
            .orElseThrow(() -> new BusinessException(
                "Type de document introuvable : " + typeDocumentId));

        List<BulkUploadItemResultDto> details = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (MultipartFile file : files)
        {
            String nomFichier = file.getOriginalFilename();
            try
            {
                DocumentUploadDto dto = new DocumentUploadDto();
                dto.setTitre(nomFichier);
                dto.setTypeDocumentId(typeDocumentId);
                dto.setUploadedById(uploadedById);
                dto.setAccess(made.archive.entite.TypeAccess.PRIVE);
                dto.setIntegrityLevel(made.archive.entite.IntegrityLevel.STANDARD);

                DocumentUploadResultDto result = documentUploadService.upload(file, dto);
                success++;

                details.add(BulkUploadItemResultDto.builder()
                    .nomFichier(nomFichier)
                    .typeDocument(typeDocument.getNom())
                    .status("SUCCESS")
                    .documentId(result.getDocumentId())
                    .build());

                log.info("[BulkSameType] OK : {}", nomFichier);
            }
            catch (Exception e)
            {
                failed++;
                details.add(BulkUploadItemResultDto.builder()
                    .nomFichier(nomFichier)
                    .typeDocument(typeDocument.getNom())
                    .status("FAILED")
                    .erreur(e.getMessage())
                    .build());

                log.warn("[BulkSameType] ERREUR {} : {}", nomFichier, e.getMessage());
            }
        }

        return BulkUploadReportDto.builder()
            .total(files.size())
            .success(success)
            .failed(failed)
            .details(details)
            .build();
    }
}
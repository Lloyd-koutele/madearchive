package made.archive.controller;

import lombok.RequiredArgsConstructor;
import made.archive.dto.BulkUploadReportDto;
import made.archive.dto.CsvTemplateRequestDto;
import made.archive.dto.DocumentUploadDto;
import made.archive.dto.DocumentUploadResultDto;
import made.archive.service.document.BulkUploadMultiTypeService;
import made.archive.service.document.BulkUploadSameTypeService;
import made.archive.service.document.CsvTemplateService;
import made.archive.service.document.DocumentUploadeService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/editor")
@RequiredArgsConstructor
public class DocumentController
{
    private final DocumentUploadeService documentUploadeService;
    private final BulkUploadMultiTypeService bulkUploadMultiTypeService;
    private final BulkUploadSameTypeService bulkUploadSameTypeService;
    private final CsvTemplateService csvTemplateService;

    /**
     * Upload d'un seul document.
     * multipart/form-data : file + dto en JSON
     */
    @Secured("ROLE_EDITOR")
    @PostMapping(value = "/docs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
        @RequestPart("file") MultipartFile file,
        @RequestPart("dto") DocumentUploadDto dto)
    {
        try
        {
            DocumentUploadResultDto result = documentUploadeService.upload(file, dto);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body("Erreur lors de l'upload : " + e.getMessage());
        }
    }

    /**
     * Upload en masse — même type de document.
     * multipart/form-data : files[] + typeDocumentId + uploadedById
     */
    @Secured("ROLE_EDITOR")
    @PostMapping(value = "/docs/bulk/same-type", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBulkSameType(
        @RequestPart("files") List<MultipartFile> files,
        @RequestParam("typeDocumentId") Long typeDocumentId,
        @RequestParam("uploadedById") UUID uploadedById)
    {
        try
        {
            BulkUploadReportDto report = bulkUploadSameTypeService
                .uploadAll(files, typeDocumentId, uploadedById);
            return ResponseEntity.ok(report);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body("Erreur upload en masse : " + e.getMessage());
        }
    }

    /**
     * Upload en masse — types différents via CSV ou Excel + zip.
     * multipart/form-data : metaFile (csv/xlsx) + zipFile + uploadedById
     */
    @Secured("ROLE_EDITOR")
    @PostMapping(value = "/docs/bulk/multi-type", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBulkMultiType(
        @RequestPart("metaFile") MultipartFile metaFile,
        @RequestPart("zipFile") MultipartFile zipFile,
        @RequestParam("uploadedById") UUID uploadedById)
    {
        // Validation présence des deux fichiers
        if (metaFile == null || metaFile.isEmpty())
        {
            return ResponseEntity.badRequest()
                .body("Le fichier CSV ou Excel est obligatoire");
        }
        if (zipFile == null || zipFile.isEmpty())
        {
            return ResponseEntity.badRequest()
                .body("Le fichier zip contenant les documents est obligatoire");
        }

        try
        {
            BulkUploadReportDto report = bulkUploadMultiTypeService
                .uploadAll(metaFile, zipFile, uploadedById);
            return ResponseEntity.ok(report);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body("Erreur upload multi-type : " + e.getMessage());
        }
    }

    /**
     * Génère et télécharge le template CSV.
     * Body : { typeDocumentIds: [1, 2, 3] }
     */
    @Secured("ROLE_EDITOR")
    @PostMapping("/docs/template/csv")
    public ResponseEntity<byte[]> downloadTemplateCsv(
        @RequestBody CsvTemplateRequestDto dto)
    {
        try
        {
            byte[] csvBytes = csvTemplateService.generateCsv(dto.getTypeDocumentIds());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                        .filename("template_import.csv")
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csvBytes);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Génère et télécharge le template Excel.
     * Body : { typeDocumentIds: [1, 2, 3] }
     */
    @Secured("ROLE_EDITOR")
    @PostMapping("/docs/template/excel")
    public ResponseEntity<byte[]> downloadTemplateExcel(
        @RequestBody CsvTemplateRequestDto dto)
    {
        try
        {
            byte[] excelBytes = csvTemplateService.generateExcel(dto.getTypeDocumentIds());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                        .filename("template_import.xlsx")
                        .build().toString())
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().build();
        }
    }
}
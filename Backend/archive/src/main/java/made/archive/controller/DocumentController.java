package made.archive.controller;

import lombok.RequiredArgsConstructor;
import made.archive.dto.BulkUploadReportDto;
import made.archive.dto.CsvTemplateRequestDto;
import made.archive.dto.DocumentUploadDto;
import made.archive.dto.DocumentUploadResultDto;
import made.archive.dto.SaveMetaDataDto;
import made.archive.entite.TypeDocument;
import made.archive.service.document.BulkUploadMultiTypeService;
import made.archive.service.document.BulkUploadSameTypeService;
import made.archive.service.document.CsvTemplateService;
import made.archive.service.document.DocumentMetaDataService;
import made.archive.service.document.DocumentUploadeService;
import made.archive.service.document.TypeDocumentService;
import made.archive.service.user.UserService;
import made.archive.util.TypeDocumentMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final TypeDocumentService typeDocumentService;
    private final TypeDocumentMapper typeDocumentMapper;
    private final DocumentMetaDataService documentMetaDataService;
    private final UserService userService;


    @Secured("ROLE_EDITOR")
    @GetMapping("/types-documents")
    public ResponseEntity<?> getAllTypeDocuments()
    {
        try
        {
            List<TypeDocument> typeDocuments = typeDocumentService.getAllTypeDocuments();
            return ResponseEntity.ok(typeDocumentMapper.toDtoList(typeDocuments));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération de tous les types de documents: " + e.getMessage());
        }
    }

    @Secured("ROLE_EDITOR")
    @GetMapping("/types-documents/{id}")
    public ResponseEntity<?> getTypeDocumentById(@PathVariable Long id)
    {
        try
        {
            TypeDocument typeDocument = typeDocumentService.getTypeDocumentById(id);
            if (typeDocument == null) 
            {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(typeDocumentMapper.toDto(typeDocument)); 
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération du type de document: " + e.getMessage());
        }
    }

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
/**
 * POST /api/editor/docs/{documentId}/metadata
 * Sauvegarde les métadonnées validées par l'utilisateur après OCR.
 */
@Secured("ROLE_EDITOR")
@PostMapping("/docs/{documentId}/metadata")
public ResponseEntity<?> saveMetaData(
    @PathVariable UUID documentId,
    @RequestBody SaveMetaDataDto dto,
    @AuthenticationPrincipal UserDetails userDetails)
{
    try
    {
        documentMetaDataService.saveMetaData(documentId, dto, userDetails);
        return ResponseEntity.ok("Métadonnées sauvegardées avec succès");
    }
    catch (Exception e)
    {
        return ResponseEntity.badRequest()
            .body("Erreur sauvegarde métadonnées : " + e.getMessage());
    }
}

/**
 * GET /api/editor/users
     * Liste tous les utilisateurs de la plateforme.
     * Utilisé pour le choix des membres du groupe lors de l'upload privé.
     */
    @Secured("ROLE_EDITOR")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers()
    {
        try
        {
            return ResponseEntity.ok(userService.getAllUsers());
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body("Erreur récupération utilisateurs : " + e.getMessage());
        }
    }
}
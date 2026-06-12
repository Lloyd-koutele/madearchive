package made.archive.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import made.archive.service.document.TypeDocumentService;
import made.archive.util.TypeDocumentMapper;
import made.archive.dto.TypeDocumentDto;
import made.archive.entite.TypeDocument;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class TypeDocumentController 
{
    private final TypeDocumentService typeDocumentService;

    private final TypeDocumentMapper typeDocumentMapper;

    public TypeDocumentController(TypeDocumentService typeDocumentService, TypeDocumentMapper typeDocumentMapper)
    {
        this.typeDocumentService = typeDocumentService;
        this.typeDocumentMapper = typeDocumentMapper;
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/types-documents/create")
    public ResponseEntity<?> createTypeDocument(@RequestBody TypeDocumentDto dto)
    {
        try
        {
            return ResponseEntity.ok(typeDocumentService.createTypeDocument(dto));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la création du type de document: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
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

    @Secured("ROLE_ADMIN")
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

    @Secured("ROLE_ADMIN")
    @PutMapping("/types-documents/{id}")
    public ResponseEntity<?> updateTypeDocument(@PathVariable Long id, @RequestBody TypeDocumentDto dto)
    {
        try
        {
            return ResponseEntity.ok(typeDocumentService.updateTypeDocument(id, dto));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour du type de document: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/types-documents/{id}")
    public ResponseEntity<?> deleteTypeDocumentById(@PathVariable Long id)
    {
        try
        {
            typeDocumentService.deleteTypeDocumentById(id);
            return ResponseEntity.ok("Type de document supprimé avec succès");
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression du type de document: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/types-documents/delete-list")
    public ResponseEntity<?> deleteListTypeDocuments(@RequestBody List<Long> ids)
    {
        try
        {
            typeDocumentService.deleteListTypeDocumentBestEffort(ids);
            return ResponseEntity.ok("Types de documents supprimés avec succès");
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression des types de documents: " + e.getMessage());
        }
    }
}

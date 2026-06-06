package made.archive.service.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import made.archive.repository.TypeDocumentRepository;
import made.archive.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import made.archive.entite.TypeDocument;
import made.archive.dto.MetaDataDto;
import made.archive.entite.MetaData;
import made.archive.entite.User;
import made.archive.entite.Retention;
import made.archive.exception.BusinessException;
import made.archive.dto.TypeDocumentDto;
import java.util.Optional;


@Service
public class TypeDocumentService 
{
    @Autowired
    private TypeDocumentRepository typeDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public List<TypeDocument> getAllTypeDocuments()
    {
        try
        {
            return typeDocumentRepository.findAll();
        }
        catch(Exception e)
        {
            throw new RuntimeException("Erreur lors de la récupération de tous les types de documents");
        }
    }

    @Transactional
    public TypeDocument getTypeDocumentById(Long id)
    {
        try 
        {
            return typeDocumentRepository.findById(id).orElse(null);
        } 
        catch (Exception e) 
        {
            throw new RuntimeException("Erreur lors de la récupération du type le type de documents");
        }
    }

    @Transactional
    public TypeDocument getTypeDocumentByName(String name)
    {
        try 
        {
            return typeDocumentRepository.findByNom(name).orElse(null);
        } 
        catch (Exception e) 
        {
            throw new RuntimeException("Erreur lors de la récupération du type le type de documents");
        }
    }

    @Transactional
    public void deleteTypeDocumentById(Long id) 
    {
        try {
            if (!typeDocumentRepository.existsById(id)) 
            {
                throw new BusinessException("Impossible de supprimer : le type de document avec l'ID " + id + " n'existe pas.");
            }
            boolean hasLinkedDocuments = typeDocumentRepository.existsByDocumentsNotEmptyAndId(id);
            
            if (hasLinkedDocuments) 
            {
                throw new BusinessException("Impossible de supprimer ce type de document car des documents y sont actuellement rattachés.");
            }
            typeDocumentRepository.deleteById(id);
            
        } 
        catch (BusinessException e) 
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new BusinessException("Une erreur technique est survenue lors de la tentative de suppression.", e);
        }
    }

    @Transactional
    public void deleteListTypeDocumentBestEffort(List<Long> ids) 
    {
        List<String> errors = new ArrayList<>();
    
        for (Long id : ids)
        {
            try 
            {
                deleteTypeDocumentById(id);
            } 
            catch (BusinessException e) 
            {
                errors.add("ID " + id + " : " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) 
        {
            throw new BusinessException("Certains types de documents n'ont pas pu être supprimés : " + String.join(" | ", errors));
        }
    }


    @Transactional
    public TypeDocument findById(Long id) 
    {
        try 
        {
            if (id == null) 
            {
                throw new BusinessException("L'ID est requis");
            }
            return typeDocumentRepository.findById(id)
                .orElseThrow(() -> {
                    return new BusinessException("Le type de document avec l'ID " + id + " n'existe pas.");
                });
        }
        catch (Exception e)
        {
            throw new BusinessException("Une erreur technique est survenue lors de la tentative de récupération du type de document.", e);
        }
    }

    @Transactional
    public TypeDocumentDto createTypeDocument(TypeDocumentDto dto) 
    {
        try {
            if (typeDocumentRepository.findByNom(dto.getNom()).isPresent()) {
                throw new BusinessException("Un type de document avec ce nom existe déjà");
            }
            if (dto.getMetaData() == null || dto.getMetaData().isEmpty()) {
                throw new BusinessException("Le type de document doit contenir au moins une métadonnée");
            }
            if (dto.getRetentionYears() == null || dto.getRetentionYears() <= 0) {
                throw new BusinessException("La durée de rétention en années est obligatoire et doit être supérieure à 0");
            }

            User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new BusinessException("Erreur : L'utilisateur créateur n'existe pas."));

            Retention retention = new Retention();
            retention.setRetentionYears(dto.getRetentionYears());
            retention.setPeriodGrace(dto.getPeriodGrace() != 30 ? dto.getPeriodGrace() : 30L);
            retention.setCreateAt(LocalDateTime.now()); 

            TypeDocument typeDocument = new TypeDocument();
            typeDocument.setNom(dto.getNom());
            typeDocument.setUser(user);
            typeDocument.setRetention(retention);

            List<MetaData> metaDataList = new ArrayList<>();
            for (MetaDataDto metaDto : dto.getMetaData()) 
            {
                if (metaDto.getNom() == null || metaDto.getNom().isBlank()) 
                {
                    throw new BusinessException("Le nom d'un attribut de métadonnée ne peut pas être vide");
                }

                MetaData metaData = new MetaData();
                metaData.setNom(metaDto.getNom());
                metaData.setObligatoire(metaDto.getObligatoire());
                metaData.setMetaDataType(metaDto.getMetaDataType());
                metaData.setTypeDocument(typeDocument);
                metaDataList.add(metaData);
            }
            typeDocument.setMetaData(metaDataList);
            typeDocumentRepository.save(typeDocument);

            dto.setId(typeDocument.getId());
            return dto;

        } 
        catch (BusinessException e) 
        {
            throw e;
        } 
        catch (Exception e) 
        {
            throw new BusinessException("Erreur lors de la création du type de document: " + e.getMessage());
        }
    }


    @Transactional
    public Optional<TypeDocumentDto> updateTypeDocument(Long id, TypeDocumentDto dto) 
    {
        try 
        {
            if (dto == null || id == null) 
            {
                throw new BusinessException("Données invalides");
            }

            TypeDocument typeDocument = typeDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type de document non trouvé avec l'ID: " + id));

            if (!typeDocument.getNom().equalsIgnoreCase(dto.getNom())) 
            {
                typeDocumentRepository.findByNom(dto.getNom()).ifPresent(t -> 
                {
                    throw new BusinessException("Ce nom est déjà utilisé");
                });
                typeDocument.setNom(dto.getNom());
            }

            Retention currentRetention = typeDocument.getRetention();
            if (dto.getRetentionYears() != null && dto.getRetentionYears() > 0) 
            {
                currentRetention.setRetentionYears(dto.getRetentionYears());
            }
            if (dto.getPeriodGrace() != null)  
            {
                currentRetention.setPeriodGrace(dto.getPeriodGrace());
            }

            if (dto.getMetaData() != null && !dto.getMetaData().isEmpty()) 
            {
                typeDocument.getMetaData().clear(); 
                for (MetaDataDto metaDto : dto.getMetaData()) {
                    MetaData metaData = new MetaData();
                    metaData.setNom(metaDto.getNom());
                    metaData.setObligatoire(metaDto.getObligatoire());
                    metaData.setMetaDataType(metaDto.getMetaDataType());
                    metaData.setTypeDocument(typeDocument);
                    typeDocument.getMetaData().add(metaData);
                }
            }

            typeDocumentRepository.save(typeDocument);
            dto.setId(typeDocument.getId());
            return Optional.of(dto);

        } 
        catch (BusinessException e) 
        {
            throw e;
        } 
        catch (Exception e) 
        {
            throw new BusinessException("Erreur lors de la modification: " + e.getMessage());
        }
    }
}

package made.archive.service.document;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.dto.BulkUploadItemResultDto;
import made.archive.dto.BulkUploadReportDto;
import made.archive.dto.DocumentUploadDto;
import made.archive.dto.DocumentUploadResultDto;
import made.archive.entite.IntegrityLevel;
import made.archive.entite.TypeAccess;
import made.archive.entite.TypeDocument;
import made.archive.exception.BusinessException;
import made.archive.repository.TypeDocumentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadMultiTypeService
{
    private final DocumentUploadeService documentUploadService;
    private final TypeDocumentRepository typeDocumentRepository;

    /**
     * Point d'entrée principal.
     * Reçoit le fichier CSV ou Excel + le zip des documents.
     */
    public BulkUploadReportDto uploadAll(MultipartFile metaFile,
                                          MultipartFile zipFile,
                                          UUID uploadedById)
    {
        // 1. Extraire les fichiers du zip en mémoire
        Map<String, byte[]> filesInZip = extractZip(zipFile);
        log.info("[BulkMultiType] {} fichiers extraits du zip", filesInZip.size());

        // 2. Lire les lignes du CSV ou Excel
        List<Map<String, String>> rows = parseMetaFile(metaFile);
        log.info("[BulkMultiType] {} lignes lues dans le fichier de métadonnées", rows.size());

        List<BulkUploadItemResultDto> details = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (Map<String, String> row : rows)
        {
            String nomFichier = row.get("nom_fichier");
            String typeDocumentNom = row.get("type_document");

            // Validation nom_fichier
            if (nomFichier == null || nomFichier.isBlank())
            {
                details.add(failed("", typeDocumentNom, "Colonne nom_fichier vide"));
                failed++;
                continue;
            }

            // Validation type_document
            if (typeDocumentNom == null || typeDocumentNom.isBlank())
            {
                details.add(failed(nomFichier, "", "Colonne type_document vide"));
                failed++;
                continue;
            }

            // Vérification que le fichier existe dans le zip
            if (!filesInZip.containsKey(nomFichier))
            {
                details.add(failed(nomFichier, typeDocumentNom,
                    "Fichier non trouvé dans le zip : " + nomFichier));
                failed++;
                continue;
            }

            // Vérification que le type existe en base
            Optional<TypeDocument> typeOpt = typeDocumentRepository.findByNom(typeDocumentNom);
            if (typeOpt.isEmpty())
            {
                details.add(failed(nomFichier, typeDocumentNom,
                    "Type de document introuvable : " + typeDocumentNom));
                failed++;
                continue;
            }

            // Upload
            try
            {
                TypeDocument typeDocument = typeOpt.get();
                byte[] fileBytes = filesInZip.get(nomFichier);

                DocumentUploadDto dto = new DocumentUploadDto();
                dto.setTitre(nomFichier);
                dto.setTypeDocumentId(typeDocument.getId());
                dto.setUploadedById(uploadedById);
                dto.setAccess(TypeAccess.PRIVE);
                dto.setIntegrityLevel(IntegrityLevel.STANDARD);

                MultipartFile multipartFile = toMultipartFile(nomFichier, fileBytes);
                DocumentUploadResultDto result = documentUploadService.upload(multipartFile, dto);
                success++;

                details.add(BulkUploadItemResultDto.builder()
                    .nomFichier(nomFichier)
                    .typeDocument(typeDocumentNom)
                    .status("SUCCESS")
                    .documentId(result.getDocumentId())
                    .build());

                log.info("[BulkMultiType] OK : {}", nomFichier);
            }
            catch (Exception e)
            {
                failed++;
                details.add(failed(nomFichier, typeDocumentNom, e.getMessage()));
                log.warn("[BulkMultiType] ERREUR {} : {}", nomFichier, e.getMessage());
            }
        }

        return BulkUploadReportDto.builder()
            .total(rows.size())
            .success(success)
            .failed(failed)
            .details(details)
            .build();
    }

    /**
     * Détecte automatiquement CSV ou Excel et délègue le parsing.
     */
    private List<Map<String, String>> parseMetaFile(MultipartFile file)
    {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".xlsx"))
        {
            return parseExcel(file);
        }
        return parseCsv(file);
    }

    private List<Map<String, String>> parseCsv(MultipartFile file)
    {
        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)))
        {
            String[] headers = reader.readNext();
            if (headers == null)
            {
                throw new BusinessException("CSV vide ou invalide");
            }

            String[] line;
            while ((line = reader.readNext()) != null)
            {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++)
                {
                    row.put(headers[i].trim(), i < line.length ? line[i].trim() : "");
                }
                rows.add(row);
            }
        }
        catch (BusinessException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new BusinessException("Erreur lecture CSV : " + e.getMessage(), e);
        }
        return rows;
    }

    private List<Map<String, String>> parseExcel(MultipartFile file)
    {
        List<Map<String, String>> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is))
        {
            // Lire tous les onglets
            for (int s = 0; s < workbook.getNumberOfSheets(); s++)
            {
                Sheet sheet = workbook.getSheetAt(s);
                String typeDocumentNom = sheet.getSheetName();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                // Construire la liste des entêtes
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow)
                {
                    headers.add(cell.getStringCellValue().trim());
                }

                // Lire les données à partir de la ligne 1
                for (int r = 1; r <= sheet.getLastRowNum(); r++)
                {
                    Row dataRow = sheet.getRow(r);
                    if (dataRow == null) continue;

                    // Ignorer les lignes vides
                    Cell firstCell = dataRow.getCell(0);
                    if (firstCell == null || firstCell.getStringCellValue().isBlank()) continue;

                    Map<String, String> row = new LinkedHashMap<>();
                    // Ajouter type_document automatiquement depuis le nom de l'onglet
                    row.put("type_document", typeDocumentNom);

                    for (int c = 0; c < headers.size(); c++)
                    {
                        Cell cell = dataRow.getCell(c);
                        row.put(headers.get(c), cell != null
                            ? getCellValue(cell) : "");
                    }
                    rows.add(row);
                }
            }
        }
        catch (Exception e)
        {
            throw new BusinessException("Erreur lecture Excel : " + e.getMessage(), e);
        }
        return rows;
    }

    private String getCellValue(Cell cell)
    {
        return switch (cell.getCellType())
        {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Map<String, byte[]> extractZip(MultipartFile zipFile)
    {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream()))
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                if (!entry.isDirectory())
                {
                    // Prendre uniquement le nom du fichier sans les sous-dossiers
                    String name = new java.io.File(entry.getName()).getName();
                    files.put(name, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        catch (Exception e)
        {
            throw new BusinessException("Erreur extraction zip : " + e.getMessage(), e);
        }
        return files;
    }

    /**
     * Convertit des bytes en MultipartFile pour réutiliser DocumentUploadService.
     */
    private MultipartFile toMultipartFile(String filename, byte[] bytes)
    {
        return new MockMultipartFile(
            filename, filename, null, bytes);
    }

    private BulkUploadItemResultDto failed(String nomFichier,
                                            String typeDocument,
                                            String erreur)
    {
        return BulkUploadItemResultDto.builder()
            .nomFichier(nomFichier)
            .typeDocument(typeDocument)
            .status("FAILED")
            .erreur(erreur)
            .build();
    }
}
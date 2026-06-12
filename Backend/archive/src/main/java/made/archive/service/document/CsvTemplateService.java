package made.archive.service.document;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.entite.MetaData;
import made.archive.entite.TypeDocument;
import made.archive.exception.BusinessException;
import made.archive.repository.TypeDocumentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvTemplateService
{
    private final TypeDocumentRepository typeDocumentRepository;

    /**
     * Génère un fichier CSV avec une ligne d'entête et une ligne d'exemple par type.
     */
    public byte[] generateCsv(List<Long> typeDocumentIds)
    {
        List<TypeDocument> types = loadTypes(typeDocumentIds);

        // Collecter toutes les colonnes MetaData de tous les types
        // Ordre : nom_fichier, type_document, puis toutes les MetaData sans doublon
        List<String> allMetaDataColumns = buildAllMetaDataColumns(types);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(
                 new OutputStreamWriter(out, StandardCharsets.UTF_8)))
        {
            // Ligne d'entête
            List<String> headers = new ArrayList<>();
            headers.add("nom_fichier");
            headers.add("type_document");
            headers.addAll(allMetaDataColumns);
            writer.writeNext(headers.toArray(new String[0]));

            // Une ligne d'exemple par type
            for (TypeDocument type : types)
            {
                List<String> exampleRow = new ArrayList<>();
                exampleRow.add("exemple_" + type.getNom().toLowerCase() + ".pdf");
                exampleRow.add(type.getNom());

                // Remplir les colonnes MetaData
                List<String> typeMetaNames = type.getMetaData().stream()
                    .map(MetaData::getNom).toList();

                for (String col : allMetaDataColumns)
                {
                    if (typeMetaNames.contains(col))
                    {
                        // Valeur d'exemple selon le nom de la colonne
                        exampleRow.add(buildExampleValue(col, type));
                    }
                    else
                    {
                        exampleRow.add(""); // colonne non applicable à ce type
                    }
                }
                writer.writeNext(exampleRow.toArray(new String[0]));
            }

            writer.flush();
            log.info("[Template] CSV généré pour {} types", types.size());
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new BusinessException("Erreur génération CSV : " + e.getMessage(), e);
        }
    }

    /**
     * Génère un fichier Excel avec un onglet par type.
     */
    public byte[] generateExcel(List<Long> typeDocumentIds)
    {
        List<TypeDocument> types = loadTypes(typeDocumentIds);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            // Style entête — fond bleu, texte blanc, gras
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");
            headerStyle.setFont(headerFont);

            // Style exemple — fond jaune clair pour indiquer que c'est un exemple
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font exampleFont = workbook.createFont();
            exampleFont.setItalic(true);
            exampleFont.setFontName("Arial");
            exampleStyle.setFont(exampleFont);

            // Style données — fond blanc, police Arial
            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataStyle.setFont(dataFont);

            for (TypeDocument type : types)
            {
                Sheet sheet = workbook.createSheet(type.getNom());

                // Ligne d'entête
                Row headerRow = sheet.createRow(0);
                List<String> columns = new ArrayList<>();
                columns.add("nom_fichier");
                type.getMetaData().forEach(m -> columns.add(m.getNom()));

                for (int i = 0; i < columns.size(); i++)
                {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, 6000); // largeur confortable
                }

                // Ligne d'exemple — fond jaune, italique
                Row exampleRow = sheet.createRow(1);
                Cell exNomFichier = exampleRow.createCell(0);
                exNomFichier.setCellValue(
                    "exemple_" + type.getNom().toLowerCase() + ".pdf");
                exNomFichier.setCellStyle(exampleStyle);

                List<MetaData> metaList = type.getMetaData();
                for (int i = 0; i < metaList.size(); i++)
                {
                    Cell cell = exampleRow.createCell(i + 1);
                    cell.setCellValue(buildExampleValue(
                        metaList.get(i).getNom(), type));
                    cell.setCellStyle(exampleStyle);
                }

                // Lignes vides prêtes à remplir (5 lignes par défaut)
                for (int r = 2; r < 7; r++)
                {
                    Row dataRow = sheet.createRow(r);
                    for (int c = 0; c < columns.size(); c++)
                    {
                        Cell cell = dataRow.createCell(c);
                        cell.setCellValue("");
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            workbook.write(out);
            log.info("[Template] Excel généré pour {} types", types.size());
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new BusinessException("Erreur génération Excel : " + e.getMessage(), e);
        }
    }

    private List<TypeDocument> loadTypes(List<Long> ids)
    {
        List<TypeDocument> types = typeDocumentRepository.findAllById(ids);
        if (types.isEmpty())
        {
            throw new BusinessException("Aucun type de document trouvé pour les IDs fournis");
        }
        return types;
    }

    private List<String> buildAllMetaDataColumns(List<TypeDocument> types)
    {
        List<String> columns = new ArrayList<>();
        for (TypeDocument type : types)
        {
            for (MetaData meta : type.getMetaData())
            {
                if (!columns.contains(meta.getNom()))
                {
                    columns.add(meta.getNom());
                }
            }
        }
        return columns;
    }

    private String buildExampleValue(String metaNom, TypeDocument type)
    {
        // Valeurs d'exemple génériques basées sur le nom du champ
        String nom = metaNom.toLowerCase();
        if (nom.contains("date")) return "01/01/2024";
        if (nom.contains("montant") || nom.contains("prix")) return "1500";
        if (nom.contains("numero") || nom.contains("ref")) 
            return type.getNom().toUpperCase().substring(0, 3) + "-2024-001";
        if (nom.contains("email")) return "exemple@domaine.com";
        if (nom.contains("tel") || nom.contains("phone")) return "77 123 45 67";
        return "exemple_" + metaNom;
    }
}
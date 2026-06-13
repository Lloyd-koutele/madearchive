package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.TesseractProperties;
import made.archive.entite.Document;
import made.archive.entite.MetaData;
import made.archive.entite.OcrResult;
import made.archive.entite.OcrStatus;
import made.archive.repository.MetaDataRepository;
import made.archive.repository.OcrResultRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService
{
    private final StorageService storageService;
    private final OcrResultRepository ocrResultRepository;
    private final MetaDataRepository metaDataRepository;
    private final OllamaService ollamaService;
    private final TesseractProperties tesseractProperties;
    private final Tika tika = new Tika();

    private static final String LANGUAGES = "fra+eng+ara";
    private static final int TIKA_MAX_CHARS = -1;
    // Résolution de rendu PDF → image pour Tesseract
    private static final float PDF_RENDER_DPI = 300f;

    @Transactional
    public String processDocument(Document document)
    {
        OcrResult result = new OcrResult();
        result.setDocument(document);
        result.setProcessedAt(LocalDateTime.now());
        result.setLanguage(LANGUAGES);

        String extractedText = null;

        try (InputStream inputStream = storageService.download(document.getStorageKey()))
        {
            byte[] fileBytes = inputStream.readAllBytes();
            String mimeType = tika.detect(fileBytes);
            log.info("[OCR] Type détecté : {} pour {}", mimeType, document.getStorageKey());

            extractedText = extractText(fileBytes, mimeType);

            if (extractedText == null || extractedText.isBlank())
            {
                result.setStatus(OcrStatus.SKIPPED);
                result.setTextStorageKey(null);
            }
            else
            {
                // Stocker le texte dans MinIO/S3 comme fichier .txt
                String textKey = storeOcrText(document, extractedText);
                result.setStatus(OcrStatus.SUCCESS);
                result.setTextStorageKey(textKey);
            }
        }
        catch (Exception e)
        {
            log.error("[OCR] Échec pour document {} : {}", document.getId(), e.getMessage());
            result.setStatus(OcrStatus.FAILED);
            result.setTextStorageKey(null);
        }

        ocrResultRepository.save(result);

        // Si premier document de ce type → générer les regex via Qwen
        generateRegexIfFirstDocument(document, extractedText);

        return extractedText;
    }

    private String extractText(byte[] fileBytes, String mimeType)
            throws IOException, SAXException, TikaException, TesseractException
    {
        // Images → Tess4J directement
        if (mimeType.startsWith("image/"))
        {
            return extractWithTesseract(fileBytes);
        }

        // PDF → tentative Tika d'abord
        if (mimeType.equals("application/pdf"))
        {
            String text = extractWithTika(fileBytes);
            if (text != null && !text.isBlank())
            {
                return text;
            }
            // PDF scanné → rendu page par page via PDFBox + Tess4J
            log.info("[OCR] PDF sans couche texte, rendu page par page...");
            return extractPdfWithTesseract(fileBytes);
        }

        // Word, Excel, etc. → Tika
        String text = extractWithTika(fileBytes);
        if (text != null && !text.isBlank())
        {
            return text;
        }

        // Fallback Tess4J pour tout autre format non reconnu par Tika
        log.info("[OCR] Tika vide, fallback Tess4J...");
        return extractWithTesseract(fileBytes);
    }

    private String extractWithTika(byte[] fileBytes)
            throws IOException, SAXException, TikaException
    {
        BodyContentHandler handler = new BodyContentHandler(TIKA_MAX_CHARS);
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        parser.parse(new ByteArrayInputStream(fileBytes), handler, metadata, context);
        return handler.toString();
    }

    private String extractWithTesseract(byte[] fileBytes)
            throws IOException, TesseractException
    {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (image == null)
        {
            log.warn("[OCR] ImageIO ne peut pas lire ce fichier pour Tess4J");
            return null;
        }
        return buildTesseract().doOCR(image);
    }

    /**
     * Rendu PDF page par page via PDFBox, puis OCR Tesseract sur chaque page.
     * Gère les PDF scannés multi-pages.
     */
    private String extractPdfWithTesseract(byte[] fileBytes)
            throws IOException, TesseractException
    {
        Tesseract tesseract = buildTesseract();
        StringBuilder fullText = new StringBuilder();

        try (PDDocument pdDocument = PDDocument.load(new ByteArrayInputStream(fileBytes).readAllBytes()))
        {
            PDFRenderer renderer = new PDFRenderer(pdDocument);
            int pageCount = pdDocument.getNumberOfPages();
            log.info("[OCR] PDF scanné : {} page(s) à traiter", pageCount);

            for (int page = 0; page < pageCount; page++)
            {
                BufferedImage image = renderer.renderImageWithDPI(page, PDF_RENDER_DPI);
                String pageText = tesseract.doOCR(image);
                if (pageText != null && !pageText.isBlank())
                {
                    fullText.append(pageText).append("\n");
                }
                log.info("[OCR] Page {}/{} traitée", page + 1, pageCount);
            }
        }

        return fullText.toString().trim();
    }

    private Tesseract buildTesseract()
    {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractProperties.getDataPath());
        tesseract.setLanguage(LANGUAGES);
        return tesseract;
    }

    private String storeOcrText(Document document, String text)
    {
        try
        {
            String key = "ocr/" + document.getId().toString() + ".txt";
            byte[] textBytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            storageService.uploadBytes(textBytes, key, "text/plain");
            log.info("[OCR] Texte stocké : {}", key);
            return key;
        }
        catch (Exception e)
        {
            log.error("[OCR] Impossible de stocker le texte OCR : {}", e.getMessage());
            return null;
        }
    }

    private void generateRegexIfFirstDocument(Document document, String extractedText)
    {
        if (extractedText == null || extractedText.isBlank()) return;

        Long typeDocumentId = document.getTypeDocument().getId();

        // Vérifie si c'est le premier document OCRisé avec succès pour ce type
        boolean isFirst = !ocrResultRepository
            .existsByDocument_TypeDocument_IdAndStatus(typeDocumentId, OcrStatus.SUCCESS);

        if (!isFirst) return;

        List<MetaData> metaDataSansRegex = metaDataRepository
            .findByTypeDocumentIdAndExtractionRegexIsNull(typeDocumentId);

        if (metaDataSansRegex.isEmpty()) return;

        log.info("[OCR] Premier document du type {} → génération regex pour {} champs",
                 typeDocumentId, metaDataSansRegex.size());

        ollamaService.generateRegexForMetaData(metaDataSansRegex, extractedText);
        metaDataRepository.saveAll(metaDataSansRegex);
    }
}
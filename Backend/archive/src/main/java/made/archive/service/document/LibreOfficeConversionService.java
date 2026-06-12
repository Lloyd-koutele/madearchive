package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.LibreOfficeProperties;
import made.archive.exception.PdfAConversionException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibreOfficeConversionService
{
    private final LibreOfficeProperties props;
    private final Tika tika = new Tika();

    // Formats supportés par LibreOffice
    private static final Set<String> SUPPORTED_MIME = Set.of(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet",
        "application/vnd.oasis.opendocument.presentation",
        "text/plain",
        "text/csv",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "image/bmp",
        "image/gif"
    );

    private static final Set<String> ALREADY_PDF = Set.of(
        "application/pdf"
    );

    /**
     * Convertit n'importe quel format supporté en PDF via LibreOffice headless.
     * Si le fichier est déjà un PDF, le retourne tel quel.
     */
    public byte[] convertToPdf(byte[] fileBytes, String originalFilename)
            throws PdfAConversionException
    {
        String mimeType = tika.detect(fileBytes);
        log.info("[LibreOffice] Format détecté : {} pour {}", mimeType, originalFilename);

        if (ALREADY_PDF.contains(mimeType))
        {
            log.info("[LibreOffice] Déjà un PDF, pas de conversion nécessaire");
            return fileBytes;
        }

        if (!SUPPORTED_MIME.contains(mimeType))
        {
            throw new PdfAConversionException(
                "Format non supporté : " + mimeType + " (" + originalFilename + ")"
            );
        }

        return convertWithLibreOffice(fileBytes, originalFilename);
    }

    private byte[] convertWithLibreOffice(byte[] fileBytes, String originalFilename)
            throws PdfAConversionException
    {
        // Nom unique pour éviter les conflits entre conversions parallèles
        String uniqueId = UUID.randomUUID().toString();
        String extension = extractExtension(originalFilename);
        Path workDir = Path.of(props.getWorkDir());
        Path inputFile = workDir.resolve(uniqueId + extension);
        Path outputFile = workDir.resolve(uniqueId + ".pdf");

        try
        {
            // Écrire le fichier temporaire
            Files.createDirectories(workDir);
            Files.write(inputFile, fileBytes);

            // Lancer LibreOffice headless
            ProcessBuilder pb = new ProcessBuilder(
                "libreoffice",
                "--headless",
                "--norestore",
                "--nofirststartwizard",
                "--convert-to", "pdf",
                "--outdir", workDir.toString(),
                inputFile.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(
                props.getTimeoutSeconds(), TimeUnit.SECONDS
            );

            if (!finished)
            {
                process.destroyForcibly();
                throw new PdfAConversionException(
                    "Timeout LibreOffice après " + props.getTimeoutSeconds() + "s pour : " + originalFilename
                );
            }

            int exitCode = process.exitValue();
            if (exitCode != 0)
            {
                throw new PdfAConversionException(
                    "LibreOffice a échoué (code " + exitCode + ") pour : " + originalFilename
                );
            }

            if (!Files.exists(outputFile))
            {
                throw new PdfAConversionException(
                    "LibreOffice n'a pas produit de PDF pour : " + originalFilename
                );
            }

            byte[] pdfBytes = Files.readAllBytes(outputFile);
            log.info("[LibreOffice] Conversion réussie : {} → PDF ({} bytes)",
                     originalFilename, pdfBytes.length);
            return pdfBytes;
        }
        catch (PdfAConversionException e)
        {
            throw e;
        }
        catch (IOException | InterruptedException e)
        {
            throw new PdfAConversionException(
                "Erreur conversion LibreOffice pour : " + originalFilename, e
            );
        }
        finally
        {
            // Nettoyage des fichiers temporaires
            deleteSilently(inputFile);
            deleteSilently(outputFile);
        }
    }

    private String extractExtension(String filename)
    {
        if (filename == null || !filename.contains("."))
        {
            return ".bin";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private void deleteSilently(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException e)
        {
            log.warn("[LibreOffice] Impossible de supprimer le fichier temporaire : {}", path);
        }
    }
}
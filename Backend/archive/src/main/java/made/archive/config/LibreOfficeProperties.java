package made.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "libreoffice")
public class LibreOfficeProperties
{
    // Répertoire temporaire pour les conversions
    private String workDir = "/tmp/libreoffice-work";

    // Timeout en secondes pour une conversion
    private int timeoutSeconds = 60;
}
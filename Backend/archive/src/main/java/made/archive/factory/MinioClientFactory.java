package made.archive.factory;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.MinioProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
public class MinioClientFactory
{
    private final MinioProperties props;
    private MinioClient client;

    public MinioClientFactory(MinioProperties props)
    {
        this.props = props;
    }

    /**
     * Retourne le client MinIO.
     * Construit à partir des credentials stockés dans MinioProperties.
     * Peut être appelé après mise à jour des credentials (setup web).
     */
    public MinioClient getClient()
    {
        if (client == null)
        {
            client = buildClient();
        }
        return client;
    }

    /**
     * Force la reconstruction du client — utile après
     * mise à jour des credentials via l'interface de setup.
     */
    public void refresh()
    {
        log.info("[MinIO] Reconstruction du client MinIO...");
        client = buildClient();
    }

    private MinioClient buildClient()
    {
        log.info("[MinIO] Initialisation du client MinIO : {}", props.getEndpoint());
        return MinioClient.builder()
            .endpoint(props.getEndpoint())
            .credentials(props.getAccessKey(), props.getSecretKey())
            .build();
    }
}
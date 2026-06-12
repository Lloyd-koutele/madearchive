package made.archive.service.storage;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.MinioProperties;
import made.archive.factory.MinioClientFactory;
import made.archive.service.document.StorageService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
public class MinioStorageService implements StorageService
{
    private final MinioClientFactory minioClientFactory;
    private final MinioProperties props;

    @Override
    public String upload(MultipartFile file, String typeDocument)
    {
        String key = typeDocument + "/" + LocalDate.now() + "/"
                   + UUID.randomUUID() + "/" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream())
        {
            minioClientFactory.getClient().putObject(
                PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("[MinIO] Fichier uploadé : {}", key);
            return key;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur upload MinIO : " + key, e);
        }
    }

    @Override
    public String uploadBytes(byte[] bytes, String key, String contentType)
    {
        try
        {
            minioClientFactory.getClient().putObject(
                PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("[MinIO] Bytes uploadés : {}", key);
            return key;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur uploadBytes MinIO : " + key, e);
        }
    }

    @Override
    public InputStream download(String key)
    {
        try
        {
            return minioClientFactory.getClient().getObject(
                GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur download MinIO : " + key, e);
        }
    }

    @Override
    public void delete(String key)
    {
        try
        {
            minioClientFactory.getClient().removeObject(
                RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur suppression MinIO : " + key, e);
        }
    }

    @Override
    public String uploadOriginal(MultipartFile file, String typeDocument)
    {
        String key = "original/" + typeDocument + "/" + LocalDate.now() + "/"
                   + UUID.randomUUID() + "/" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream())
        {
            minioClientFactory.getClient().putObject(
                PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("[MinIO] Original uploadé : {}", key);
            return key;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur upload original MinIO : " + key, e);
        }
    }
}
package made.archive.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.config.S3Properties;
import made.archive.service.document.StorageService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService 
{

    private final S3Client s3Client;
    private final S3Properties props;

    @Override
    public String upload(MultipartFile file, String typeDocument) 
    {
        String key = typeDocument + "/" + LocalDate.now().toString() + "/" + UUID.randomUUID().toString() + "/"+ file.getOriginalFilename();
        try 
        {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return key;
        } 
        catch (Exception e) 
        {
            throw new RuntimeException("Erreur upload S3 : " + key, e);
        }
    }

    @Override
    public InputStream download(String key) 
    {
        return s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build()
        );
    }

    @Override
    public void delete(String key) 
    {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build()
        );
    }

    @Override
    public String uploadOriginal(MultipartFile file, String typeDocument)
    {
        String key = "original/" + typeDocument + "/" + LocalDate.now() + "/"
                   + UUID.randomUUID() + "/" + file.getOriginalFilename();
        try
        {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            log.info("[S3] Original uploadé : {}", key);
            return key;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Erreur upload original S3 : " + key, e);
        }
    }

    // Ajouter uploadBytes
@Override
public String uploadBytes(byte[] bytes, String key, String contentType)
{
    try
    {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        );
        log.info("[S3] Bytes uploadés : {}", key);
        return key;
    }
    catch (Exception e)
    {
        throw new RuntimeException("Erreur uploadBytes S3 : " + key, e);
    }
}
}
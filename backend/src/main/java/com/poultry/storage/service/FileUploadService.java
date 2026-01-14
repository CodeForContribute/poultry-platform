package com.poultry.storage.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.storage.entity.FileUpload;
import com.poultry.storage.repository.FileUploadRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final MinioClient minioClient;
    private final FileUploadRepository fileUploadRepository;

    @Value("${minio.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    @Transactional
    public FileUpload uploadFile(MultipartFile file, String bucket, String referenceType,
                                  UUID referenceId, String uploadedByType, UUID uploadedById) {
        try {
            ensureBucketExists(bucket);

            String objectKey = generateObjectKey(file.getOriginalFilename());
            String checksum = calculateChecksum(file.getInputStream());

            // Upload to MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // Save metadata
            FileUpload fileUpload = FileUpload.builder()
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .checksum(checksum)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .uploadedByType(uploadedByType)
                    .uploadedById(uploadedById)
                    .isPublic(false)
                    .build();

            fileUpload = fileUploadRepository.save(fileUpload);

            log.info("File uploaded: id={}, bucket={}, key={}", fileUpload.getId(), bucket, objectKey);

            return fileUpload;

        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new BusinessException("Failed to upload file", "FILE_UPLOAD_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public String getPresignedUrl(UUID fileId) {
        FileUpload fileUpload = fileUploadRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> BusinessException.notFound("File", fileId));

        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(fileUpload.getBucket())
                    .object(fileUpload.getObjectKey())
                    .method(Method.GET)
                    .expiry(presignedUrlExpiryMinutes, TimeUnit.MINUTES)
                    .build());

            log.debug("Generated presigned URL for file: {}", fileId);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {}", fileId, e);
            throw new BusinessException("Failed to generate download URL", "PRESIGNED_URL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        FileUpload fileUpload = fileUploadRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> BusinessException.notFound("File", fileId));

        // Soft delete
        fileUpload.setDeletedAt(Instant.now());
        fileUploadRepository.save(fileUpload);

        log.info("File soft deleted: {}", fileId);
    }

    @Transactional
    public void permanentlyDeleteFile(UUID fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("File", fileId));

        try {
            // Delete from MinIO
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(fileUpload.getBucket())
                    .object(fileUpload.getObjectKey())
                    .build());

            // Delete from database
            fileUploadRepository.delete(fileUpload);

            log.info("File permanently deleted: {}", fileId);

        } catch (Exception e) {
            log.error("Failed to permanently delete file: {}", fileId, e);
            throw new BusinessException("Failed to delete file", "FILE_DELETE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public FileUpload getFile(UUID fileId) {
        return fileUploadRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> BusinessException.notFound("File", fileId));
    }

    @Transactional(readOnly = true)
    public List<FileUpload> getFilesByReference(String referenceType, UUID referenceId) {
        return fileUploadRepository.findByReferenceTypeAndReferenceIdAndDeletedAtIsNull(referenceType, referenceId);
    }

    @Transactional(readOnly = true)
    public InputStream downloadFile(UUID fileId) {
        FileUpload fileUpload = fileUploadRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> BusinessException.notFound("File", fileId));

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(fileUpload.getBucket())
                    .object(fileUpload.getObjectKey())
                    .build());

        } catch (Exception e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new BusinessException("Failed to download file", "FILE_DOWNLOAD_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucket, e);
            throw new BusinessException("Storage initialization failed", "BUCKET_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateObjectKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private String calculateChecksum(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            log.warn("Failed to calculate checksum", e);
            return null;
        }
    }
}

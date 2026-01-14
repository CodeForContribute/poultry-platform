package com.poultry.storage.repository;

import com.poultry.storage.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {

    Optional<FileUpload> findByIdAndDeletedAtIsNull(UUID id);

    Optional<FileUpload> findByBucketAndObjectKeyAndDeletedAtIsNull(String bucket, String objectKey);

    List<FileUpload> findByReferenceTypeAndReferenceIdAndDeletedAtIsNull(String referenceType, UUID referenceId);

    List<FileUpload> findByUploadedByTypeAndUploadedByIdAndDeletedAtIsNull(String uploadedByType, UUID uploadedById);
}

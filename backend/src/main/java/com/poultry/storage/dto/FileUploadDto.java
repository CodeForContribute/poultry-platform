package com.poultry.storage.dto;

import com.poultry.storage.entity.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDto {
    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
  private String referenceType;
    private UUID referenceId;
    private Boolean isPublic;
    private String presignedUrl;
    private Instant createdAt;
    private Instant expiresAt;

    public static FileUploadDto fromEntity(FileUpload entity) {
        return FileUploadDto.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .sizeBytes(entity.getSizeBytes())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .isPublic(entity.getIsPublic())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }

    public static FileUploadDto fromEntity(FileUpload entity, String presignedUrl) {
        FileUploadDto dto = fromEntity(entity);
        dto.setPresignedUrl(presignedUrl);
        return dto;
    }
}

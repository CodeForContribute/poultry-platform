package com.poultry.storage.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.storage.entity.FileUpload;
import com.poultry.storage.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload and management APIs")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file", description = "Upload a file to the specified bucket")
    public ResponseEntity<ApiResponse<FileUpload>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bucket") String bucket,
            @RequestParam("referenceType") String referenceType,
            @RequestParam("referenceId") UUID referenceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileUpload fileUpload = fileUploadService.uploadFile(
                file, bucket, referenceType, referenceId,
                principal.getUserType(), principal.getId());

        return ResponseEntity.ok(ApiResponse.success(fileUpload));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Get metadata for a specific file")
    public ResponseEntity<ApiResponse<FileUpload>> getFile(@PathVariable UUID fileId) {
        FileUpload fileUpload = fileUploadService.getFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(fileUpload));
    }

    @GetMapping("/{fileId}/url")
    @Operation(summary = "Get presigned download URL", description = "Get a temporary URL to download the file")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDownloadUrl(@PathVariable UUID fileId) {
        String url = fileUploadService.getPresignedUrl(fileId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @GetMapping("/by-reference")
    @Operation(summary = "Get files by reference", description = "Get all files for a specific reference")
    public ResponseEntity<ApiResponse<List<FileUpload>>> getFilesByReference(
            @RequestParam String referenceType,
            @RequestParam UUID referenceId) {

        List<FileUpload> files = fileUploadService.getFilesByReference(referenceType, referenceId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file", description = "Soft delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID fileId) {
        fileUploadService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

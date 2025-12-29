package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.FileResponse;
import com.UsdtWallet.UsdtWallet.model.entity.File;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * Upload a file for a specific entity (milestone, project, etc.)
     * 
     * @param file The file to upload
     * @param entityType Type of entity (MILESTONE, PROJECT, etc.)
     * @param entityId ID of the entity
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("entityType") String entityType,
        @RequestParam("entityId") UUID entityId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            File.FileEntityType type = File.FileEntityType.valueOf(entityType.toUpperCase());
            
            FileResponse response = fileStorageService.uploadFile(
                file, 
                currentUser.getId(), 
                type, 
                entityId
            );
            
            log.info("File uploaded by user {}: {}", currentUser.getId(), response.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
            
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }
    
    /**
     * Get all files for a specific entity
     */
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<FileResponse>> getFilesByEntity(
        @PathVariable String entityType,
        @PathVariable UUID entityId
    ) {
        try {
            File.FileEntityType type = File.FileEntityType.valueOf(entityType.toUpperCase());
            List<FileResponse> files = fileStorageService.getFilesByEntity(type, entityId);
            return ResponseEntity.ok(files);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Download a file
     */
    @GetMapping("/download/{entityType}/{entityId}/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
        @PathVariable String entityType,
        @PathVariable String entityId,
        @PathVariable String filename
    ) {
        try {
            Path filePath = fileStorageService.getFilePath(entityType, entityId, filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.error("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (IOException e) {
                log.warn("Could not determine file type for {}", filename);
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
                
        } catch (Exception e) {
            log.error("File download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a file
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
        @PathVariable UUID fileId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            fileStorageService.deleteFile(fileId, currentUser.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
                
        } catch (IOException e) {
            log.error("File deletion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to delete file: " + e.getMessage()));
        }
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}

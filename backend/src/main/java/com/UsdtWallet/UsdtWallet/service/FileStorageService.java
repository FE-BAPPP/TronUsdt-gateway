package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.File;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.model.dto.response.FileResponse;
import com.UsdtWallet.UsdtWallet.repository.FileRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${file.max.size:10485760}") // 10MB default
    private long maxFileSize;
    
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf",
        "application/zip", "application/x-zip-compressed",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    );
    
    /**
     * Upload a file and save metadata to database
     */
    @Transactional
    public FileResponse uploadFile(
        MultipartFile file, 
        UUID uploaderId, 
        File.FileEntityType entityType, 
        UUID entityId
    ) throws IOException {
        
        // Validation
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : "";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + extension;
        
        // Create directory structure: uploads/{entityType}/{entityId}/
        Path entityDir = Paths.get(uploadDir, entityType.name().toLowerCase(), entityId.toString());
        Files.createDirectories(entityDir);
        
        // Save file to disk
        Path filePath = entityDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File uploaded successfully: {}", filePath.toString());
        
        // Save metadata to database
        File fileEntity = File.builder()
            .uploadedBy(uploaderId)
            .entityType(entityType)
            .entityId(entityId)
            .fileUrl("/api/files/download/" + entityType.name().toLowerCase() + "/" + entityId + "/" + uniqueFilename)
            .fileName(originalFilename)
            .fileSize(file.getSize())
            .mimeType(contentType)
            .build();
        
        fileEntity = fileRepository.save(fileEntity);
        
        return convertToResponse(fileEntity);
    }
    
    /**
     * Get all files for an entity
     */
    public List<FileResponse> getFilesByEntity(File.FileEntityType entityType, UUID entityId) {
        List<File> files = fileRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return files.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get file by ID
     */
    public File getFileById(UUID fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
    }
    
    /**
     * Delete file (soft delete - remove from DB and disk)
     */
    @Transactional
    public void deleteFile(UUID fileId, UUID requesterId) throws IOException {
        File file = getFileById(fileId);
        
        // Check permission: only uploader or admin can delete
        if (!file.getUploadedBy().equals(requesterId)) {
            User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!requester.isAdmin()) {
                throw new IllegalArgumentException("Not authorized to delete this file");
            }
        }
        
        // Extract filename from URL: /api/files/download/{type}/{id}/{filename}
        String fileUrl = file.getFileUrl();
        String[] parts = fileUrl.split("/");
        if (parts.length >= 6) {
            String entityType = parts[4];
            String entityId = parts[5];
            String filename = parts[6];
            
            Path filePath = Paths.get(uploadDir, entityType, entityId, filename);
            Files.deleteIfExists(filePath);
            log.info("Deleted file from disk: {}", filePath);
        }
        
        fileRepository.delete(file);
    }
    
    /**
     * Get physical file path for download
     */
    public Path getFilePath(String entityType, String entityId, String filename) {
        return Paths.get(uploadDir, entityType, entityId, filename);
    }
    
    /**
     * Convert File entity to FileResponse DTO
     */
    private FileResponse convertToResponse(File file) {
        User uploader = userRepository.findById(file.getUploadedBy()).orElse(null);
        
        return FileResponse.builder()
            .id(file.getId())
            .uploadedBy(file.getUploadedBy())
            .uploaderName(uploader != null ? uploader.getFullName() : "Unknown")
            .entityType(file.getEntityType().name())
            .entityId(file.getEntityId())
            .fileUrl(file.getFileUrl())
            .fileName(file.getFileName())
            .fileSize(file.getFileSize())
            .mimeType(file.getMimeType())
            .createdAt(file.getCreatedAt())
            .build();
    }
}

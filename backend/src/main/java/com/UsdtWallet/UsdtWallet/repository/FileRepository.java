package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    
    /**
     * Find all files for a specific entity (e.g., all files for a milestone)
     */
    List<File> findByEntityTypeAndEntityId(File.FileEntityType entityType, UUID entityId);
    
    /**
     * Find all files uploaded by a specific user
     */
    List<File> findByUploadedBy(UUID uploadedBy);
    
    /**
     * Count files for a specific entity
     */
    long countByEntityTypeAndEntityId(File.FileEntityType entityType, UUID entityId);
}

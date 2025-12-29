package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Skill;
import com.UsdtWallet.UsdtWallet.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 🏷️ SKILL CONTROLLER
 * 
 * Quản lý skills trong hệ thống
 * - Admin: Thêm/xóa/sửa skills
 * - All users: Xem danh sách skills
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Slf4j
public class SkillController {

    private final SkillRepository skillRepository;

    /**
     * 📋 GET /api/skills - Lấy tất cả skills
     * 
     * Dùng cho dropdown selection khi post job hoặc create profile
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Skill>>> getAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    /**
     * ➕ POST /api/skills - Thêm skill mới (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Skill>> createSkill(@RequestBody SkillRequest request) {
        try {
            // Check duplicate
            if (skillRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
                throw new RuntimeException("Skill already exists: " + request.getName());
            }

            Skill skill = Skill.builder()
                .name(request.getName())
                .build();

            skill = skillRepository.save(skill);
            log.info("✅ Skill created: {}", skill.getName());

            return ResponseEntity.ok(ApiResponse.success("Skill created successfully", skill));
        } catch (Exception e) {
            log.error("Failed to create skill: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Skill>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * ❌ DELETE /api/skills/{id} - Xóa skill (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable UUID id) {
        try {
            Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

            skillRepository.delete(skill);
            log.info("✅ Skill deleted: {}", skill.getName());

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Skill deleted successfully")
                .build());
        } catch (Exception e) {
            log.error("Failed to delete skill: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * DTO for skill creation
     */
    @lombok.Data
    public static class SkillRequest {
        private String name;
    }
}

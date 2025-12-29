package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "employer_id", nullable = false)
    private UUID employerId;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private ProjectType jobType;
    
    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;
    
    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;
    
    @Column(name = "budget", nullable = false, precision = 20, scale = 2)
    private BigDecimal budget;
    
    @Column(length = 10)
    private String currency = "USDT";
    
    // ✅ ADD: Duration field
    @Column(name = "duration", length = 100)
    private String duration;
    
    @Column(name = "deadline")
    private LocalDate deadline;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;
    
    // ✅ ADD: Type field (NOT NULL in DB) - duplicates job_type but DB has both
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ProjectType type;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Many-to-Many with Skills
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "job_skills",
        joinColumns = @JoinColumn(name = "job_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> requiredSkills = new HashSet<>();
    
    /**
     * 🆕 Job complexity/size
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "complexity")
    @Builder.Default
    private JobComplexity complexity = JobComplexity.SMALL;
    
    @Column(name = "estimated_milestones")
    private Integer estimatedMilestones;
    
    public enum JobStatus {
        OPEN,           // ✅ Job đang mở, nhận proposals
        CLOSED,         // ✅ Job đóng, không nhận proposal nữa  
        CANCELLED,      // ✅ Job bị hủy
        IN_PROGRESS     // ✅ Job đang có project (proposal đã awarded)
    }
    
    public enum ProjectType {
        FIXED_PRICE,
        HOURLY
    }
    
    public enum JobComplexity {
        SMALL,   // 1 milestone (banner design, quick fix)
        MEDIUM,  // 2-5 milestones (landing page, small app)
        LARGE    // 5+ milestones (full project, complex system)
    }
}
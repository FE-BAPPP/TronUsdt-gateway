package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.response.ConversationResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Conversation;
import com.UsdtWallet.UsdtWallet.model.entity.Job;
import com.UsdtWallet.UsdtWallet.model.entity.Project;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.ConversationRepository;
import com.UsdtWallet.UsdtWallet.repository.JobRepository;
import com.UsdtWallet.UsdtWallet.repository.ProjectRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    /**
     * 🆕 Create conversation for a project (auto-called when project is created)
     */
    @Transactional
    public Conversation createConversationForProject(UUID projectId) {
        // Check if conversation already exists
        if (conversationRepository.findByProjectId(projectId).isPresent()) {
            log.warn("Conversation already exists for project: {}", projectId);
            return conversationRepository.findByProjectId(projectId).get();
        }

        // Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Create conversation
        Conversation conversation = Conversation.builder()
            .projectId(projectId)
            .jobId(project.getJobId())
            .createdAt(LocalDateTime.now())
            .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("✅ Conversation created: {} for project: {}", saved.getId(), projectId);

        return saved;
    }

    /**
     * Get conversation by project ID
     */
    public Conversation getConversationByProjectId(UUID projectId) {
        return conversationRepository.findByProjectId(projectId)
            .orElseThrow(() -> new RuntimeException("No conversation found for this project"));
    }

    /**
     * Get conversation by job ID (for pre-project discussions)
     */
    public Conversation getConversationByJobId(UUID jobId) {
        return conversationRepository.findByJobId(jobId)
            .orElseThrow(() -> new RuntimeException("No conversation found for this job"));
    }

    /**
     * Check if conversation exists for project
     */
    public boolean conversationExistsForProject(UUID projectId) {
        return conversationRepository.findByProjectId(projectId).isPresent();
    }

    /**
     * Lấy tất cả conversations của user
     */
    public List<ConversationResponse> getUserConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findAll().stream()
                .filter(conv -> isUserPartOfConversation(conv, userId))
                .collect(Collectors.toList());

        return conversations.stream()
                .map(conv -> mapToResponse(conv, userId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy conversation theo ID với validation
     */
    public ConversationResponse getConversationById(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!isUserPartOfConversation(conversation, userId)) {
            throw new RuntimeException("You are not authorized to access this conversation");
        }

        return mapToResponse(conversation, userId);
    }

    /**
     * Check if user is part of conversation
     */
    private boolean isUserPartOfConversation(Conversation conversation, UUID userId) {
        if (conversation.getJobId() != null) {
            Optional<Job> job = jobRepository.findById(conversation.getJobId());
            if (job.isPresent()) {
                return job.get().getEmployerId().equals(userId);
            }
        }
        
        if (conversation.getProjectId() != null) {
            Optional<Project> project = projectRepository.findById(conversation.getProjectId());
            if (project.isPresent()) {
                return project.get().getEmployerId().equals(userId) || 
                       project.get().getFreelancerId().equals(userId);
            }
        }
        
        return false;
    }

    /**
     * Map entity to response
     */
    private ConversationResponse mapToResponse(Conversation conversation, UUID currentUserId) {
        ConversationResponse.ConversationResponseBuilder builder = ConversationResponse.builder()
                .id(conversation.getId())
                .jobId(conversation.getJobId())
                .projectId(conversation.getProjectId())
                .createdAt(conversation.getCreatedAt());

        // Add job/project title
        if (conversation.getJobId() != null) {
            jobRepository.findById(conversation.getJobId()).ifPresent(job -> {
                builder.title(job.getTitle());
                builder.type("JOB");
            });
        }
        
        if (conversation.getProjectId() != null) {
            projectRepository.findById(conversation.getProjectId()).ifPresent(project -> {
                builder.title(project.getJob().getTitle());
                builder.type("PROJECT");
                
                // Add other party info
                UUID otherPartyId = project.getEmployerId().equals(currentUserId) 
                        ? project.getFreelancerId() 
                        : project.getEmployerId();
                
                userRepository.findById(otherPartyId).ifPresent(user -> {
                    builder.otherPartyId(user.getId());
                    builder.otherPartyName(user.getFullName());
                    builder.otherPartyAvatar(user.getAvatar());
                });
            });
        }

        return builder.build();
    }
}

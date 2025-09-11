package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;

  
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@RequestParam(value = "userId", required = false) String userIdStr,
                                         @RequestParam(value = "username", required = false) String username,
                                         HttpServletResponse response) {
        log.info("🔔 Starting SSE stream for user: {} (username: {})", userIdStr, username);
        
        // Set CORS headers for SSE
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Cache-Control", "no-cache");
        
        UUID parsedUserId;
        if (userIdStr != null) {
            try {
                parsedUserId = UUID.fromString(userIdStr);
            } catch (Exception e) {
                log.warn("Invalid userId format: {}, using random UUID", userIdStr);
                parsedUserId = UUID.randomUUID();
            }
        } else {
            parsedUserId = UUID.randomUUID();
        }
        final UUID userId = parsedUserId;
        
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); 
        
        // Add CORS headers for SSE
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data("SSE connection established"));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE message", e);
        }
        
        notificationService.addUserConnection(userId, emitter);
        
        emitter.onCompletion(() -> {
            log.debug("SSE completed for user: {}", userId);
            notificationService.removeUserConnection(userId, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user: {}", userId);
            notificationService.removeUserConnection(userId, emitter);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE error for user: {}", userId, ex);
            notificationService.removeUserConnection(userId, emitter);
        });
        
        return emitter;
    }

    /**
     * Get unread notifications count
     */
    @GetMapping("/count")
    public Long getUnreadCount(@AuthenticationPrincipal UserPrincipal currentUser) {
        return notificationService.getUnreadCount(currentUser.getId());
    }

    /**
     * Mark notification as read
     */
    @PostMapping("/read/{notificationId}")
    public void markAsRead(@PathVariable Long notificationId, 
                          @AuthenticationPrincipal UserPrincipal currentUser) {
        notificationService.markAsRead(notificationId, currentUser.getId());
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal UserPrincipal currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
    }


}
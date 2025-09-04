package com.UsdtWallet.UsdtWallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final Optional<JavaMailSender> mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String fromAddress;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.brand.name:USDT Wallet}")
    private String brandName;

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody, String textFallback) {
        if (mailSender.isEmpty()) {
            log.warn("No JavaMailSender configured. Skipping email to {} with subject '{}'.", toEmail, subject);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textFallback != null ? textFallback : htmlBody.replaceAll("<[^>]*>", ""), htmlBody);
            mailSender.get().send(mimeMessage);
        } catch (Exception e) {
            log.warn("HTML email send failed, falling back to plaintext: {}", e.getMessage());
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(textFallback != null ? textFallback : htmlBody.replaceAll("<[^>]*>", ""));
                mailSender.get().send(message);
            } catch (Exception ex) {
                log.error("Failed to send fallback plaintext email to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        if (mailSender.isEmpty()) {
            log.warn("No JavaMailSender configured. Skipping password reset email to {}.", toEmail);
            return;
        }
        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String resetLink = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + encodedToken;

            String subject = "Reset your " + brandName + " password";

            String html = "" +
                "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#0f172a;\">" +
                "  <h2 style=\"margin:0 0 16px;\">" + brandName + "</h2>" +
                "  <p>We received a request to reset your password.</p>" +
                "  <p>This link will expire in <strong>15 minutes</strong>.</p>" +
                "  <p style=\"margin:24px 0;\"><a href=\"" + resetLink + "\" style=\"background:#2563eb;color:#fff;text-decoration:none;padding:12px 16px;border-radius:6px;display:inline-block;\">Reset password</a></p>" +
                "  <p>If the button doesn’t work, copy and paste this URL into your browser:</p>" +
                "  <p style=\"word-break:break-all;color:#334155\">" + resetLink + "</p>" +
                "  <hr style=\"border:none;border-top:1px solid #e2e8f0;margin:24px 0\"/>" +
                "  <p style=\"color:#64748b;font-size:12px\">If you didn’t request this, you can ignore this email.</p>" +
                "</div>";

            String text = "We received a request to reset your password.\n" +
                "This link will expire in 15 minutes.\n\n" + resetLink + "\n\n" +
                "If you didn’t request this, you can ignore this email.";

            sendHtmlEmail(toEmail, subject, html, text);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordChangedEmail(String toEmail) {
        if (mailSender.isEmpty()) {
            log.warn("No JavaMailSender configured. Skipping password changed email to {}.", toEmail);
            return;
        }
        try {
            String subject = brandName + ": Your password was changed";
            String html = "" +
                "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#0f172a;\">" +
                "  <h2 style=\"margin:0 0 16px;\">" + brandName + "</h2>" +
                "  <p>Your account password was just changed.</p>" +
                "  <p style=\"color:#64748b\">If this wasn’t you, please contact support immediately.</p>" +
                "</div>";

            String text = brandName + ": Your account password was just changed. If this wasn’t you, please contact support immediately.";

            sendHtmlEmail(toEmail, subject, html, text);
            log.info("Password changed email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password changed email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}

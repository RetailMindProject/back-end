package com.example.back_end.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails (verification, password reset, etc.).
 *
 * <p>Uses Spring's JavaMailSender for email delivery.
 * All methods are async to avoid blocking the request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.name:POS System}")
    private String appName;

    /**
     * Send email verification link.
     *
     * @param toEmail Recipient email address
     * @param userName User's name (first name or full name)
     * @param token Verification token
     */
    @Async
    public void sendEmailVerification(String toEmail, String userName, String token) {
        try {
            String subject = "Verify Your Email - " + appName;
            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            String htmlContent = buildEmailVerificationHtml(userName, verificationLink);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Email verification sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email verification to: {}", toEmail, e);
        }
    }

    /**
     * Send registration verification link for new customer accounts.
     * Uses /verify-registration endpoint instead of /verify-email.
     *
     * @param toEmail Recipient email address
     * @param userName User's name (first name or full name)
     * @param token Verification token
     */
    @Async
    public void sendRegistrationVerification(String toEmail, String userName, String token) {
        try {
            String subject = "Verify Your Email - " + appName;
            String verificationLink = frontendUrl + "/verify-registration?token=" + token;

            String htmlContent = buildRegistrationVerificationHtml(userName, verificationLink);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Registration verification email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send registration verification email to: {}", toEmail, e);
        }
    }

    /**
     * Send password reset link.
     *
     * @param toEmail Recipient email address
     * @param userName User's name (first name or full name)
     * @param token Password reset token
     */
    @Async
    public void sendPasswordReset(String toEmail, String userName, String token) {
        try {
            String subject = "Reset Your Password - " + appName;
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String htmlContent = buildPasswordResetHtml(userName, resetLink);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    /**
     * Send HTML email.
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param htmlContent HTML content
     * @throws MessagingException if email sending fails
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }

    /**
     * Send plain text email (fallback).
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param text Plain text content
     */
    private void sendTextEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    /**
     * Build HTML content for email verification.
     */
    private String buildEmailVerificationHtml(String userName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>""" + appName + """
            </h1>
                    </div>
                    <div class="content">
                        <h2>Welcome, """ + userName + """
            !</h2>
                        <p>Thank you for registering with """ + appName + """
            . To complete your registration, please verify your email address.</p>
                        
                        <p style="text-align: center;">
                            <a href=\"""" + verificationLink + """
            " class="button">Verify Email Address</a>
                        </p>
                        
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background-color: #eee; padding: 10px; border-radius: 3px;">
                            """ + verificationLink + """
            
                        </p>
                        
                        <div class="warning">
                            <strong>Important:</strong> This link will expire in 24 hours.
                        </div>
                        
                        <p>If you didn't create an account with us, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 """ + appName + """
            . All rights reserved.</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * Build HTML content for registration verification (new customer accounts).
     */
    private String buildRegistrationVerificationHtml(String userName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }
                    .success { background-color: #d4edda; border-left: 4px solid #28a745; padding: 10px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>""" + appName + """
            </h1>
                    </div>
                    <div class="content">
                        <h2>Welcome to """ + appName + """
            , """ + userName + """
            !</h2>
                        <p>Thank you for creating your account. You're almost there! To complete your registration and activate your account, please verify your email address.</p>
                        
                        <div class="success">
                            <strong>One more step:</strong> Click the button below to verify your email and activate your account.
                        </div>
                        
                        <p style="text-align: center;">
                            <a href=\"""" + verificationLink + """
            " class="button">Verify Email & Activate Account</a>
                        </p>
                        
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background-color: #eee; padding: 10px; border-radius: 3px;">
                            """ + verificationLink + """
            
                        </p>
                        
                        <div class="warning">
                            <strong>Important:</strong> This link will expire in 24 hours. After verification, you'll be able to login immediately.
                        </div>
                        
                        <p>If you didn't create an account with us, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 """ + appName + """
            . All rights reserved.</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * Build HTML content for password reset.
     */
    private String buildPasswordResetHtml(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #2196F3; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }
                    .security { background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 10px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>""" + appName + """
            </h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hello """ + userName + """
            ,</p>
                        <p>We received a request to reset your password. Click the button below to create a new password:</p>
                        
                        <p style="text-align: center;">
                            <a href=\"""" + resetLink + """
            " class="button">Reset Password</a>
                        </p>
                        
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background-color: #eee; padding: 10px; border-radius: 3px;">
                            """ + resetLink + """
            
                        </p>
                        
                        <div class="warning">
                            <strong>Important:</strong> This link will expire in 1 hour.
                        </div>
                        
                        <div class="security">
                            <strong>Security Notice:</strong> If you didn't request a password reset, please ignore this email or contact support if you're concerned about your account security.
                        </div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 """ + appName + """
            . All rights reserved.</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}


package com.example.back_end.modules.messages.controller;

import com.example.back_end.modules.messages.dto.*;
import com.example.back_end.modules.messages.service.FileStorageService;
import com.example.back_end.modules.messages.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER', 'INVENTORY_MANAGER','CUSTOMER')")
public class MessageController {

    private final MessageService messageService;
    private final FileStorageService fileStorageService;

    /**
     * إرسال رسالة جديدة مع ملفات مرفقة (أو بدون)
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @RequestParam("toUserId") Integer toUserId,
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            @RequestParam(value = "parentMessageId", required = false) Long parentMessageId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        SendMessageRequestDTO request = new SendMessageRequestDTO();
        request.setToUserId(toUserId);
        request.setTitle(title);
        request.setBody(body);
        request.setParentMessageId(parentMessageId);

        MessageResponseDTO response = messageService.sendMessageWithAttachments(request, files);
        return ResponseEntity.ok(response);
    }

    /**
     * الرد على رسالة
     */
    @PostMapping("/reply")
    public ResponseEntity<MessageResponseDTO> replyToMessage(
            @Valid @RequestBody ReplyMessageRequestDTO request) {
        MessageResponseDTO response = messageService.replyToMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * تحميل ملف مرفق
     */
    @GetMapping("/attachments/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * جلب الرسائل المستلمة (Inbox)
     */
    @GetMapping("/inbox")
    public ResponseEntity<MessagesListResponseDTO> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        MessagesListResponseDTO response = messageService.getInbox(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * جلب الرسائل المرسلة (Sent)
     */
    @GetMapping("/sent")
    public ResponseEntity<MessagesListResponseDTO> getSentMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        MessagesListResponseDTO response = messageService.getSentMessages(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * جلب قائمة المستقبلين المتاحين للإرسال
     */
    @GetMapping("/recipients")
    public ResponseEntity<List<UserBasicDTO>> getRecipients() {
        List<UserBasicDTO> recipients = messageService.getAvailableRecipients();
        return ResponseEntity.ok(recipients);
    }

    /**
     * جلب عدد الرسائل غير المقروءة للمستخدم الحالي
     */
    @GetMapping("/unread-count")
    // Allow any authenticated user (including CUSTOMER) to access unread count
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'CUSTOMER')")
    public ResponseEntity<UnreadCountDTO> getUnreadCount() {
        UnreadCountDTO unreadCount = messageService.getUnreadCount();
        return ResponseEntity.ok(unreadCount);
    }

    /**
     * جلب رسالة معينة
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable Long messageId) {
        MessageDTO message = messageService.getMessageById(messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * جلب الردود على رسالة معينة
     */
    @GetMapping("/{messageId}/replies")
    public ResponseEntity<List<MessageDTO>> getMessageReplies(@PathVariable Long messageId) {
        List<MessageDTO> replies = messageService.getMessageReplies(messageId);
        return ResponseEntity.ok(replies);
    }

    /**
     * تحديد رسالة كمقروءة
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * أرشفة رسالة
     */
    @PutMapping("/{messageId}/archive")
    public ResponseEntity<Void> archiveMessage(@PathVariable Long messageId) {
        messageService.archiveMessage(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * البحث في الرسائل
     */
    @GetMapping("/search")
    public ResponseEntity<MessagesListResponseDTO> searchMessages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        MessagesListResponseDTO response = messageService.searchMessages(keyword, page, size);
        return ResponseEntity.ok(response);
    }
}

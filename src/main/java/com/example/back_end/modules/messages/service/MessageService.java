package com.example.back_end.modules.messages.service;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.messages.dto.*;
import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.entity.Message.MessageStatus;
import com.example.back_end.modules.messages.entity.MessageAttachment;
import com.example.back_end.modules.messages.mapper.MessageMapper;
import com.example.back_end.modules.messages.repository.MessageAttachmentRepository;
import com.example.back_end.modules.messages.repository.MessageRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final FileStorageService fileStorageService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    @Transactional
    public MessageResponseDTO sendMessage(SendMessageRequestDTO request) {
        User fromUser = getCurrentUser();

        // التحقق من المستخدم المستلم
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new CustomException("Recipient user not found"));

        // التحقق من أن المستلم نشط
        if (!toUser.getIsActive()) {
            throw new CustomException("Recipient user is inactive");
        }

        // إنشاء الرسالة
        Message message = new Message();
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setTitle(request.getTitle());
        message.setBody(request.getBody());
        message.setStatus(MessageStatus.SENT);

        // إذا كانت رد على رسالة
        if (request.getParentMessageId() != null) {
            Message parentMessage = messageRepository.findById(request.getParentMessageId())
                    .orElseThrow(() -> new CustomException("Parent message not found"));
            message.setParentMessage(parentMessage);
            message.setTitle("Re: " + parentMessage.getTitle());
        }

        Message savedMessage = messageRepository.save(message);

        log.info("Message sent from user {} to user {}", fromUser.getEmail(), toUser.getEmail());

        MessageDTO messageDTO = messageMapper.toDTO(savedMessage, null);

        return MessageResponseDTO.builder()
                .message(messageDTO)
                .status("success")
                .notification("Message sent successfully to " + toUser.getFirstName())
                .build();
    }

    @Transactional
    public MessageResponseDTO replyToMessage(ReplyMessageRequestDTO request) {
        User currentUser = getCurrentUser();

        // جلب الرسالة الأصلية
        Message parentMessage = messageRepository.findByIdAndUserAccess(
                        request.getParentMessageId(),
                        currentUser.getId())
                .orElseThrow(() -> new CustomException("Message not found or access denied"));

        // تحديد المستلم (المرسل الأصلي)
        User toUser = parentMessage.getFromUser();
        if (toUser.getId().equals(currentUser.getId())) {
            // إذا كان المستخدم الحالي هو المرسل الأصلي، نرسل للمستلم الأصلي
            toUser = parentMessage.getToUser();
        }

        // إنشاء الرد
        Message reply = new Message();
        reply.setFromUser(currentUser);
        reply.setToUser(toUser);
        reply.setTitle("Re: " + parentMessage.getTitle());
        reply.setBody(request.getBody());
        reply.setStatus(MessageStatus.SENT);
        reply.setParentMessage(parentMessage);

        Message savedReply = messageRepository.save(reply);

        log.info("Reply sent from user {} to user {} for message {}",
                currentUser.getEmail(), toUser.getEmail(), parentMessage.getId());

        MessageDTO messageDTO = messageMapper.toDTO(savedReply, null);

        return MessageResponseDTO.builder()
                .message(messageDTO)
                .status("success")
                .notification("Reply sent successfully")
                .build();
    }

    @Transactional
    public MessageResponseDTO sendMessageWithAttachments(
            SendMessageRequestDTO request,
            List<MultipartFile> files) {

        User fromUser = getCurrentUser();

        // التحقق من المستخدم المستلم
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new CustomException("Recipient user not found"));

        if (!toUser.getIsActive()) {
            throw new CustomException("Recipient user is inactive");
        }

        // إنشاء الرسالة
        Message message = new Message();
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setTitle(request.getTitle());
        message.setBody(request.getBody());
        message.setStatus(MessageStatus.SENT);

        if (request.getParentMessageId() != null) {
            Message parentMessage = messageRepository.findById(request.getParentMessageId())
                    .orElseThrow(() -> new CustomException("Parent message not found"));
            message.setParentMessage(parentMessage);
            message.setTitle("Re: " + parentMessage.getTitle());
        }

        Message savedMessage = messageRepository.save(message);

        // رفع الملفات المرفقة
        List<MessageAttachment> attachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file);
                    String checksum = fileStorageService.calculateChecksum(file);

                    MessageAttachment attachment = new MessageAttachment();
                    attachment.setMessage(savedMessage);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFileUrl("/api/messages/attachments/download/" + fileName);
                    attachment.setMimeType(file.getContentType());
                    attachment.setFileSize(file.getSize());
                    attachment.setChecksum(checksum);

                    attachments.add(attachmentRepository.save(attachment));
                }
            }
        }

        log.info("Message sent from user {} to user {} with {} attachments",
                fromUser.getEmail(), toUser.getEmail(), attachments.size());

        MessageDTO messageDTO = messageMapper.toDTO(savedMessage, attachments);

        return MessageResponseDTO.builder()
                .message(messageDTO)
                .status("success")
                .notification("Message sent successfully to " + toUser.getFirstName())
                .build();
    }

    @Transactional(readOnly = true)
    public MessagesListResponseDTO getInbox(int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage = messageRepository
                .findByToUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        List<MessageDTO> messages = messagesPage.getContent().stream()
                .map(msg -> messageMapper.toDTO(msg, attachmentRepository.findByMessageId(msg.getId())))
                .collect(Collectors.toList());

        Integer unreadCount = messageRepository
                .countByToUserIdAndStatus(currentUser.getId(), MessageStatus.SENT);

        return MessagesListResponseDTO.builder()
                .messages(messages)
                .totalMessages((int) messagesPage.getTotalElements())
                .unreadCount(unreadCount)
                .currentPage(page)
                .totalPages(messagesPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public MessagesListResponseDTO getSentMessages(int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage = messageRepository
                .findByFromUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        List<MessageDTO> messages = messagesPage.getContent().stream()
                .map(msg -> messageMapper.toDTO(msg, attachmentRepository.findByMessageId(msg.getId())))
                .collect(Collectors.toList());

        return MessagesListResponseDTO.builder()
                .messages(messages)
                .totalMessages((int) messagesPage.getTotalElements())
                .unreadCount(0)
                .currentPage(page)
                .totalPages(messagesPage.getTotalPages())
                .build();
    }

    @Transactional
    public MessageDTO getMessageById(Long messageId) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findByIdAndUserAccess(messageId, currentUser.getId())
                .orElseThrow(() -> new CustomException("Message not found or access denied"));

        // تحديث حالة القراءة إذا كان المستلم
        if (message.getToUser().getId().equals(currentUser.getId()) &&
                message.getStatus() == MessageStatus.SENT) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(ZonedDateTime.now());
            messageRepository.save(message);
        }

        List<MessageAttachment> attachments = attachmentRepository.findByMessageId(messageId);

        return messageMapper.toDTO(message, attachments);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessageReplies(Long messageId) {
        User currentUser = getCurrentUser();

        // التحقق من وصول المستخدم للرسالة
        messageRepository.findByIdAndUserAccess(messageId, currentUser.getId())
                .orElseThrow(() -> new CustomException("Message not found or access denied"));

        List<Message> replies = messageRepository.findByParentMessageIdOrderByCreatedAtAsc(messageId);

        return replies.stream()
                .map(reply -> messageMapper.toDTO(reply,
                        attachmentRepository.findByMessageId(reply.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long messageId) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findByIdAndUserAccess(messageId, currentUser.getId())
                .orElseThrow(() -> new CustomException("Message not found or access denied"));

        if (message.getToUser().getId().equals(currentUser.getId())) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(ZonedDateTime.now());
            messageRepository.save(message);
        }
    }

    @Transactional
    public void archiveMessage(Long messageId) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findByIdAndUserAccess(messageId, currentUser.getId())
                .orElseThrow(() -> new CustomException("Message not found or access denied"));

        message.setStatus(MessageStatus.ARCHIVED);
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public MessagesListResponseDTO searchMessages(String keyword, int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage = messageRepository
                .searchMessages(currentUser.getId(), keyword, pageable);

        List<MessageDTO> messages = messagesPage.getContent().stream()
                .map(msg -> messageMapper.toDTO(msg, attachmentRepository.findByMessageId(msg.getId())))
                .collect(Collectors.toList());

        return MessagesListResponseDTO.builder()
                .messages(messages)
                .totalMessages((int) messagesPage.getTotalElements())
                .unreadCount(0)
                .currentPage(page)
                .totalPages(messagesPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserBasicDTO> getAvailableRecipients() {
        User currentUser = getCurrentUser();

        return userRepository.findAll().stream()
                .filter(User::getIsActive) // فقط المستخدمين النشطاء
                .filter(user -> !user.getId().equals(currentUser.getId())) // استثناء المستخدم الحالي
                .map(user -> UserBasicDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .role(user.getRole()) // UserRole من الـ entity
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount() {
        User currentUser = getCurrentUser();

        Integer unreadCount = messageRepository
                .countByToUserIdAndStatus(currentUser.getId(), MessageStatus.SENT);

        return UnreadCountDTO.builder()
                .unreadCount(unreadCount)
                .build();
    }
}

package com.example.back_end.modules.messages.mapper;

import com.example.back_end.modules.messages.dto.MessageAttachmentDTO;
import com.example.back_end.modules.messages.dto.MessageDTO;
import com.example.back_end.modules.messages.dto.UserBasicDTO;
import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.entity.MessageAttachment;
import com.example.back_end.modules.messages.repository.MessageRepository;
import com.example.back_end.modules.register.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final MessageRepository messageRepository;

    public MessageDTO toDTO(Message message, List<MessageAttachment> attachments) {
        if (message == null) {
            return null;
        }

        return MessageDTO.builder()
                .id(message.getId())
                .fromUser(toUserBasicDTO(message.getFromUser()))
                .toUser(toUserBasicDTO(message.getToUser()))
                .title(message.getTitle())
                .body(message.getBody())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .parentMessageId(message.getParentMessage() != null ?
                        message.getParentMessage().getId() : null)
                .attachments(attachments != null ?
                        attachments.stream()
                                .map(this::toAttachmentDTO)
                                .collect(Collectors.toList()) : null)
                .repliesCount(messageRepository.countByParentMessageId(message.getId()))
                .build();
    }

    public UserBasicDTO toUserBasicDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserBasicDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public MessageAttachmentDTO toAttachmentDTO(MessageAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return MessageAttachmentDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .checksum(attachment.getChecksum())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
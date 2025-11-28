package com.example.back_end.modules.messages.dto;

import com.example.back_end.modules.messages.entity.Message.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    private Long id;
    private UserBasicDTO fromUser;
    private UserBasicDTO toUser;
    private String title;
    private String body;
    private MessageStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime readAt;
    private Long parentMessageId;
    private List<MessageAttachmentDTO> attachments;
    private Integer repliesCount;
}
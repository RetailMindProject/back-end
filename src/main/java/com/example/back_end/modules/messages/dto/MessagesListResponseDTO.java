package com.example.back_end.modules.messages.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessagesListResponseDTO {
    private List<MessageDTO> messages;
    private Integer totalMessages;
    private Integer unreadCount;
    private Integer currentPage;
    private Integer totalPages;
}
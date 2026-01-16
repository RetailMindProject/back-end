package com.example.back_end.modules.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagChatRequest {
    private String message;
    private String projectId;
    private String conversationId;
    private String language;
    private String channel;
    private Map<String, Object> metadata;
}

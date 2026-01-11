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
public class RagChatResponse {
    private String answer;
    private String dataSource;
    private String conversationId;
    private Map<String, Object> extra;
}

package com.example.back_end.modules.rag.service;

import com.example.back_end.modules.rag.dto.RagChatRequest;
import com.example.back_end.modules.rag.dto.RagChatResponse;

public interface RagChatService {

    RagChatResponse chat(RagChatRequest request, String bearerToken);
}


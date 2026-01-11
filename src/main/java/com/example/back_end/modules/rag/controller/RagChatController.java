package com.example.back_end.modules.rag.controller;

import com.example.back_end.modules.rag.dto.RagChatRequest;
import com.example.back_end.modules.rag.dto.RagChatResponse;
import com.example.back_end.modules.rag.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/rag")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/chat")
    public ResponseEntity<RagChatResponse> chat(@RequestBody RagChatRequest request,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        RagChatResponse response = ragChatService.chat(request, authorizationHeader);
        return ResponseEntity.ok(response);
    }
}


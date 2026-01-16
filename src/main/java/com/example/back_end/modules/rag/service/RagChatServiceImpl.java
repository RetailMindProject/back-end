package com.example.back_end.modules.rag.service;

import com.example.back_end.modules.rag.dto.RagChatRequest;
import com.example.back_end.modules.rag.dto.RagChatResponse;
import com.example.back_end.modules.rag.dto.RagServiceRequest;
import com.example.back_end.modules.rag.dto.RagServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private final WebClient ragWebClient;

    @Value("${rag.service.project-id:test_project}")
    private String defaultProjectId;

    @Override
    public RagChatResponse chat(RagChatRequest request, String bearerToken) {
        String projectId = request.getProjectId() != null ? request.getProjectId() : defaultProjectId;

        RagServiceRequest serviceRequest = RagServiceRequest.builder()
                .text(request.getMessage())
                .limit(5)
                .build();

        RagServiceResponse serviceResponse = ragWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/nlp/index/answer/{projectId}")
                        .build(projectId))
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(serviceRequest), RagServiceRequest.class)
                .retrieve()
                .bodyToMono(RagServiceResponse.class)
                .block();

        if (serviceResponse == null) {
            return RagChatResponse.builder()
                    .answer("Assistant is temporarily unavailable. Please try again.")
                    .build();
        }

        return RagChatResponse.builder()
                .answer(serviceResponse.getAnswer())
                .dataSource(serviceResponse.getData_source())
                .build();
    }
}


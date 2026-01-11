package com.example.back_end.modules.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagServiceResponse {
    private String signal;
    private String answer;
    private String data_source;
    private Object products;
    private String confidence_note;
}


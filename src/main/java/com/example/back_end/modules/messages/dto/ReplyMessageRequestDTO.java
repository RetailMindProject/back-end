package com.example.back_end.modules.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyMessageRequestDTO {

    @NotNull(message = "Parent message ID is required")
    private Long parentMessageId;

    @NotBlank(message = "Message body is required")
    private String body;
}
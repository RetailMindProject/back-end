package com.example.back_end.modules.messages.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadAttachmentRequestDTO {

    @NotNull(message = "Message ID is required")
    private Long messageId;

    @NotNull(message = "File is required")
    private MultipartFile file;
}
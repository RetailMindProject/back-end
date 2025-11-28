package com.example.back_end.modules.messages.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private String checksum;
    private ZonedDateTime uploadedAt;
}
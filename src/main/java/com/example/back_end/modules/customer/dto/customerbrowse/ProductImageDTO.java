package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

@Builder
public record ProductImageDTO(
        Long id,
        String url,
        String mimeType,
        String title,
        String altText,
        boolean isPrimary,
        int sortOrder
) {}


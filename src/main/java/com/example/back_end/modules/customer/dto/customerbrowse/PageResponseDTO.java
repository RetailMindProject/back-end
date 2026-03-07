package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

import java.util.List;

@Builder
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}


package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

@Builder
public record CategoryChildDTO(
        Long id,
        String name,
        Long parentId
) {}


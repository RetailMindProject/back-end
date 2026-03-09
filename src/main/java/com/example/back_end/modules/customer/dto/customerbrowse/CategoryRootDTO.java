package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

@Builder
public record CategoryRootDTO(
        Long id,
        String name,
        boolean hasChildren
) {}


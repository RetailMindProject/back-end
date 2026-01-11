package com.example.back_end.modules.catalog.product.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchTermResponseDTO {
    private Long id;
    private Long productId;
    private String term;
    private Instant createdAt;
}


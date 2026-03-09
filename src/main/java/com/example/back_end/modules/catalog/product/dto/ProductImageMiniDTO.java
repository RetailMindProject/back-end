package com.example.back_end.modules.catalog.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageMiniDTO {
    private String url;
    private String altText;
}


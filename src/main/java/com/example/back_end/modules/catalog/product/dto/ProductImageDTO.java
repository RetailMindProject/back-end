package com.example.back_end.modules.catalog.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {
    private Long mediaId;
    private String url;
    private String mimeType;
    private String title;
    private String altText;
    private Integer sortOrder;
    private Boolean isPrimary;
}


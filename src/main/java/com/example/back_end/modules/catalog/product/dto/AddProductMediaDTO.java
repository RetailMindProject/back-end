package com.example.back_end.modules.catalog.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddProductMediaDTO {

    @NotBlank(message = "Image URL is required")
    private String url;

    private String mimeType;

    private String title;

    private String altText;

    private Integer sortOrder;

    @NotNull(message = "isPrimary is required")
    private Boolean isPrimary;
}


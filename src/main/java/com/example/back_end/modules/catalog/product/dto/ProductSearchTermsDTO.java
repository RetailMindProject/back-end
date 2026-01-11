package com.example.back_end.modules.catalog.product.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchTermsDTO {
    private List<String> terms;
}


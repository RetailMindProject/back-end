package com.example.back_end.modules.publicapi.products.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProductListResponse {
    private List<PublicProductDTO> items;
    private int total;
}


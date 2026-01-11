package com.example.back_end.modules.publicapi.products.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean available;
}

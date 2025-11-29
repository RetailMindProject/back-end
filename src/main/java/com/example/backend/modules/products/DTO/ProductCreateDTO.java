package com.example.back_end.modules.products.DTO;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCreateDTO {
    @Size(max = 64) private String sku;
    @NotBlank @Size(max = 120) private String name;
    @Size(max = 80) private String brand;
    @Size(max = 500) private String description;
    @NotNull @PositiveOrZero private BigDecimal defaultCost;
    @NotNull @PositiveOrZero private BigDecimal defaultPrice;
    @NotNull @PositiveOrZero @Digits(integer = 3, fraction = 2) private BigDecimal taxRate; // e.g., 15.00 = 15%
    @Size(max = 20) private String unit;
    @PositiveOrZero private BigDecimal wholesalePrice;
    private Boolean isActive; // optional
}

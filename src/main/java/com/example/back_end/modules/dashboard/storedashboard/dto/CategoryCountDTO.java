package com.example.back_end.modules.dashboard.storedashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryCountDTO {
    private String name;
    private Long value;  // product count
}


package com.example.back_end.modules.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetaDTO {
    private List<CashierOption> cashiers;
    private List<String> statuses;
    private LocalDate earliestOrderDate;
    private LocalDate latestOrderDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashierOption {
        private Long id;
        private String name;
    }
}

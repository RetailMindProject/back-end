package com.example.back_end.modules.customer.dto.customerbrowse;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum ProductSortKey {
    RELEVANCE,
    PRICE_ASC,
    PRICE_DESC,
    NEWEST;

    public static ProductSortKey fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return RELEVANCE;
        }
        try {
            return ProductSortKey.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Set<String> allowed = Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());
            throw new IllegalArgumentException("Invalid sortKey '" + raw + "'. Allowed values: " + allowed);
        }
    }
}


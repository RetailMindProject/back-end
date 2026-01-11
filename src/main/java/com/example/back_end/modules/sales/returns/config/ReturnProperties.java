package com.example.back_end.modules.sales.returns.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pos.returns")
public class ReturnProperties {

    /**
     * Return window in days counted from orders.paid_at.
     */
    private int windowDays = 14;
}


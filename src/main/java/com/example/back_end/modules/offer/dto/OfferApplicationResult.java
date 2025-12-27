package com.example.back_end.modules.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of applying ORDER offer to an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplicationResult {

    // Was offer applied?
    private Boolean offerApplied;

    // Offer details
    private Long offerId;
    private String offerCode;
    private String offerTitle;

    // Discount amounts
    private BigDecimal originalSubtotal;         // Subtotal before order discount
    private BigDecimal discountAmount;           // Order-level discount
    private BigDecimal subtotalAfterDiscount;    // Subtotal after order discount
}
package com.example.back_end.modules.offer.mapper;

import com.example.back_end.modules.offer.dto.OfferResponseDTO;
import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.entity.OfferBundle;
import com.example.back_end.modules.offer.entity.OfferCategory;
import com.example.back_end.modules.offer.entity.OfferProduct;
import com.example.back_end.modules.register.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OfferMapper {

    public OfferResponseDTO toResponseDTO(Offer offer) {
        if (offer == null) {
            return null;
        }

        OfferResponseDTO.OfferResponseDTOBuilder builder = OfferResponseDTO.builder()
                .id(offer.getId())
                .code(offer.getCode())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .offerType(offer.getOfferType())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .startAt(offer.getStartAt())
                .endAt(offer.getEndAt())
                .isActive(offer.getIsActive())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt());

        // Add created by name if available
        if (offer.getCreatedBy() != null) {
            builder.createdByName(offer.getCreatedBy().getFirstName() + " " + offer.getCreatedBy().getLastName());
        }

        // Map based on offer type
        switch (offer.getOfferType()) {
            case PRODUCT:
                Set<OfferProduct> offerProducts = offer.getOfferProducts();
                builder.products(mapProducts(offerProducts));
                break;
            case CATEGORY:
                builder.categories(mapCategories(offer.getOfferCategories()));
                break;
            case ORDER:
                if (offer.getOrderOffer() != null) {
                    builder.minOrderAmount(offer.getOrderOffer().getMinOrderAmount())
                            .applyOnce(offer.getOrderOffer().getApplyOnce());
                }
                break;
            case BUNDLE:
                builder.bundleItems(mapBundleItems(offer.getOfferBundles()));
                break;
        }

        return builder.build();
    }

    private List<OfferResponseDTO.ProductInfoDTO> mapProducts(Set<OfferProduct> offerProducts) {
        if (offerProducts == null || offerProducts.isEmpty()) {  // ← أضف isEmpty()
            return new ArrayList<>();  // ← return empty list مش null
        }
        return offerProducts.stream()
                .map(op -> OfferResponseDTO.ProductInfoDTO.builder()
                        .id(op.getProduct().getId())
                        .sku(op.getProduct().getSku())
                        .name(op.getProduct().getName())
                        .price(op.getProduct().getDefaultPrice())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OfferResponseDTO.CategoryInfoDTO> mapCategories(Set<OfferCategory> offerCategories) {
        if (offerCategories == null) {
            return null;
        }
        return offerCategories.stream()
                .map(oc -> OfferResponseDTO.CategoryInfoDTO.builder()
                        .id(oc.getCategory().getId())
                        .name(oc.getCategory().getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OfferResponseDTO.BundleItemInfoDTO> mapBundleItems(Set<OfferBundle> offerBundles) {
        if (offerBundles == null) {
            return null;
        }
        return offerBundles.stream()
                .map(ob -> OfferResponseDTO.BundleItemInfoDTO.builder()
                        .productId(ob.getProduct().getId())
                        .sku(ob.getProduct().getSku())
                        .name(ob.getProduct().getName())
                        .requiredQty(ob.getRequiredQty())
                        .price(ob.getProduct().getDefaultPrice())
                        .build())
                .collect(Collectors.toList());
    }

    public List<OfferResponseDTO> toResponseDTOList(List<Offer> offers) {
        if (offers == null) {
            return null;
        }
        return offers.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
}
package com.example.back_end.modules.offer.entity;

import com.example.back_end.modules.register.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"offerProducts", "offerCategories", "offerBundles", "orderOffer", "createdBy"})
@EqualsAndHashCode(exclude = {"offerProducts", "offerCategories", "offerBundles", "orderOffer", "createdBy"})
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 40, unique = true)
    private String code;

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", length = 20, nullable = false)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20, nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default  // ← مهم جداً!
    private Set<OfferProduct> offerProducts = new HashSet<>();  // ← initialize

    // وأضف getter
    public Set<OfferProduct> getOfferProducts() {
        if (offerProducts == null) {
            offerProducts = new HashSet<>();
        }
        return offerProducts;
    }

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OfferCategory> offerCategories = new HashSet<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OfferBundle> offerBundles = new HashSet<>();

    @OneToOne(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderOffer orderOffer;

    // Enums
    public enum OfferType {
        PRODUCT,
        CATEGORY,
        ORDER,
        BUNDLE
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    // Helper methods for bidirectional relationships
    public void addOfferProduct(OfferProduct offerProduct) {
        offerProducts.add(offerProduct);
        offerProduct.setOffer(this);
    }

    public void removeOfferProduct(OfferProduct offerProduct) {
        offerProducts.remove(offerProduct);
        offerProduct.setOffer(null);
    }

    public void addOfferCategory(OfferCategory offerCategory) {
        offerCategories.add(offerCategory);
        offerCategory.setOffer(this);
    }

    public void removeOfferCategory(OfferCategory offerCategory) {
        offerCategories.remove(offerCategory);
        offerCategory.setOffer(null);
    }

    public void addOfferBundle(OfferBundle offerBundle) {
        offerBundles.add(offerBundle);
        offerBundle.setOffer(this);
    }

    public void removeOfferBundle(OfferBundle offerBundle) {
        offerBundles.remove(offerBundle);
        offerBundle.setOffer(null);
    }
}
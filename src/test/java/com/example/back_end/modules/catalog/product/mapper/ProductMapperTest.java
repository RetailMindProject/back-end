package com.example.back_end.modules.catalog.product.mapper;

import com.example.back_end.modules.catalog.product.dto.ProductSimpleDTO;
import com.example.back_end.modules.catalog.product.entity.Media;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    @Test
    void toSimpleDTO_includesImageMiniWithPathOnlyUrl() {
        Media media = new Media();
        media.setUrl("https://example.com/picture/katchap.jpg");
        media.setAltText("Katchap");

        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU1");
        product.setName("Katchap");
        product.setDefaultCost(BigDecimal.ZERO);
        product.setDefaultPrice(new BigDecimal("5.00"));

        ProductMedia pm = new ProductMedia();
        pm.setProduct(product);
        pm.setMedia(media);
        pm.setIsPrimary(true);
        product.setProductMedia(Set.of(pm));

        ProductSimpleDTO dto = ProductMapper.toSimpleDTO(product);

        assertThat(dto.getImage()).isNotNull();
        assertThat(dto.getImage().getUrl()).isEqualTo("/picture/katchap.jpg");
        assertThat(dto.getImage().getAltText()).isEqualTo("Katchap");
    }
}


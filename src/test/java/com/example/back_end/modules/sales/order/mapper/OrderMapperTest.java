package com.example.back_end.modules.sales.order.mapper;

import com.example.back_end.modules.catalog.product.entity.Media;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    @Test
    void toOrderItemResponse_includesProductMiniWithPrimaryImagePathOnly() {
        Media media = new Media();
        media.setId(1L);
        media.setUrl("http://localhost:8081/picture/Tea.jpg");
        media.setAltText("Tea product");

        Product product = new Product();
        product.setId(55L);
        product.setName("Tea");

        ProductMedia pm = new ProductMedia();
        pm.setProduct(product);
        pm.setMedia(media);
        pm.setIsPrimary(true);
        product.setProductMedia(Set.of(pm));

        OrderItem item = new OrderItem();
        item.setId(7L);
        item.setProduct(product);
        item.setUnitPrice(new BigDecimal("2.00"));
        item.setQuantity(new BigDecimal("3.00"));
        item.setLineTotal(new BigDecimal("6.00"));

        OrderMapper mapper = new OrderMapper();
        OrderDTO.OrderItemResponse dto = mapper.toOrderItemResponse(item);

        assertThat(dto.getProductId()).isEqualTo(55L);
        assertThat(dto.getProduct()).isNotNull();
        assertThat(dto.getProduct().getId()).isEqualTo(55L);
        assertThat(dto.getProduct().getName()).isEqualTo("Tea");
        assertThat(dto.getProduct().getImage()).isNotNull();
        assertThat(dto.getProduct().getImage().getUrl()).isEqualTo("/picture/Tea.jpg");
        assertThat(dto.getProduct().getImage().getAltText()).isEqualTo("Tea product");
    }
}

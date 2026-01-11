package com.example.back_end.modules.publicapi.products.service;

import com.example.back_end.modules.publicapi.products.dto.PublicProductDTO;
import com.example.back_end.modules.publicapi.products.dto.PublicProductListResponse;

public interface PublicProductService {
    PublicProductListResponse search(String query, int limit);
    PublicProductListResponse suggestions(String query, int limit);
    PublicProductDTO getById(Long id);
    boolean isAvailable(Long id);
}

package com.example.back_end.modules.customer.service.customerbrowse;

import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.customer.dto.customerbrowse.CategoryChildDTO;
import com.example.back_end.modules.customer.dto.customerbrowse.CategoryRootDTO;
import com.example.back_end.modules.customer.repository.customerbrowse.CategoryCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryCustomerService {

    private final CategoryCustomerRepository categoryCustomerRepository;

    @Transactional(readOnly = true)
    public List<CategoryRootDTO> getRootCategories() {
        List<Category> roots = categoryCustomerRepository.findRootCategories();
        return roots.stream()
                .map(c -> CategoryRootDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .hasChildren(categoryCustomerRepository.hasChildren(c.getId()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryChildDTO> getChildren(Long parentId) {
        return categoryCustomerRepository.findChildren(parentId)
                .stream()
                .map(c -> CategoryChildDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .parentId(parentId)
                        .build())
                .toList();
    }
}


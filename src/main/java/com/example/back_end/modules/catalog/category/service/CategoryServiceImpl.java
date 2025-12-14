package com.example.back_end.modules.catalog.category.service;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryCreateDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryUpdateDTO;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.mapper.CategoryMapper;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
import com.example.back_end.modules.catalog.category.dto.CategoryTreeDTO;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    @Override
    public List<CategoryDTO> getAll() {
        return categories.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryDTO> getByProductId(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.getCategories().size(); // ensure initialized
        // include direct categories + their parents if they exist
        LinkedHashSet<Category> expanded = new LinkedHashSet<>();
        for (Category c : product.getCategories()) {
            expanded.add(c);
            if (c.getParent() != null) {
                expanded.add(c.getParent());
            }
        }
        return expanded.stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDTO create(CategoryCreateDTO dto) {
        String name = dto.getName().trim();
        if (categories.existsByName(name)) {
            throw new IllegalArgumentException("Category name already exists");
        }
        Category parent = null;
        if (dto.getParentId() != null) {
            parent = categories.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent not found: " + dto.getParentId()));
        }
        Category saved = categories.save(Category.builder()
                .name(name)
                .parent(parent)
                .build());
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDTO update(Long id, CategoryUpdateDTO dto) {
        Category cat = categories.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));

        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (!name.equals(cat.getName()) && categories.existsByName(name)) {
                throw new IllegalArgumentException("Category name already exists");
            }
            cat.setName(name);
        }

        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            Category parent = categories.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent not found: " + dto.getParentId()));
            cat.setParent(parent);
        } else if (dto.getParentId() == null) {
            // allow clearing parent when explicitly null
            cat.setParent(null);
        }

        Category saved = categories.save(cat);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!categories.existsById(id)) {
            throw new EntityNotFoundException("Category not found: " + id);
        }
        categories.deleteById(id);
    }

    @Override
    public List<CategoryTreeDTO> getTree() {
        List<Category> all = categories.findAll(Sort.by(Sort.Direction.ASC, "name"));
        Map<Long, CategoryTreeDTO> map = new HashMap<>();
        List<CategoryTreeDTO> roots = new ArrayList<>();

        for (Category c : all) {
            CategoryTreeDTO dto = CategoryTreeDTO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .parentId(c.getParent() != null ? c.getParent().getId() : null)
                    .children(new ArrayList<>())
                    .build();
            map.put(c.getId(), dto);
        }

        for (Category c : all) {
            CategoryTreeDTO dto = map.get(c.getId());
            if (c.getParent() != null && map.containsKey(c.getParent().getId())) {
                map.get(c.getParent().getId()).getChildren().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }
}


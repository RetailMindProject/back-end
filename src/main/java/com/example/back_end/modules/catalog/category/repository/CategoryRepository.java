package com.example.back_end.modules.catalog.category.repository;
import com.example.back_end.modules.catalog.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);
}

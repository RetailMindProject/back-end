package com.example.back_end.modules.customer.repository.customerbrowse;

import com.example.back_end.modules.catalog.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryCustomerRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.name ASC")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.name ASC")
    List<Category> findChildren(@Param("parentId") Long parentId);

    @Query("SELECT (COUNT(c) > 0) FROM Category c WHERE c.parent.id = :parentId")
    boolean hasChildren(@Param("parentId") Long parentId);
}


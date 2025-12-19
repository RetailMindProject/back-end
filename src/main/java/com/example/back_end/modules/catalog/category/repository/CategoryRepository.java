package com.example.back_end.modules.catalog.category.repository;

import com.example.back_end.modules.catalog.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findAllParentCategories();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.name")
    List<Category> findSubCategoriesByParentId(@Param("parentId") Long parentId);

    @Query("""
            SELECT DISTINCT c
            FROM Category c
            LEFT JOIN FETCH c.children
            WHERE c.parent IS NULL
            ORDER BY c.name
            """)
    List<Category> findAllCategoriesWithSubCategories();
    // أو غيّر اسمها لـ findAllCategoriesWithChildren لو حاب

    /**
     * Count products linked to a specific category
     */
    @Query("""
            SELECT COUNT(p)
            FROM Category c
            LEFT JOIN c.products p
            WHERE c.id = :categoryId
            """)
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}

package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findByUrl(String url);
    
    @Query("SELECT m FROM Media m WHERE m.id NOT IN (SELECT pm.media.id FROM ProductMedia pm)")
    List<Media> findOrphanedMedia();
}


package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    java.util.Optional<Media> findByUrl(String url);
}


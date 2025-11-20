package com.example.back_end.modules.login.reporsitory;

import com.example.back_end.modules.login.entity.uesr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface repository extends JpaRepository<uesr, Integer> {

    Optional<uesr> findByEmail(String email);

    Optional<uesr> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);
}
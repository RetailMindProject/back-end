package com.example.back_end.modules.register.repository;

import com.example.back_end.modules.register.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUserId(Integer userId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}


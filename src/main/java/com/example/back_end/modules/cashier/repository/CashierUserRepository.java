package com.example.back_end.modules.cashier.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.back_end.modules.register.entity.User;


import java.util.List;
import java.util.Optional;

@Repository
public interface CashierUserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all cashiers
     */
    @Query("SELECT u FROM User u WHERE u.role = 'CASHIER' ORDER BY u.firstName, u.lastName")
    List<User> findAllCashiers();

    /**
     * Find active cashiers
     */
    @Query("SELECT u FROM User u WHERE u.role = 'CASHIER' AND u.isActive = true ORDER BY u.firstName, u.lastName")
    List<User> findActiveCashiers();

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
}

package com.example.back_end.modules.customer.service;

import com.example.back_end.exception.DuplicateResourceException;
import com.example.back_end.modules.customer.dto.CashierCustomerCreateRequest;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.CustomerRepository;
import com.example.back_end.modules.register.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashierCustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<Customer> searchByPhone(String phone) {
        return customerRepository.findByPhone(normalize(phone));
    }

    /**
     * Cashier flow: create both users + customers records (like /api/auth/register/customer).
     *
     * Rules:
     * - users.email is unique and required.
     * - customers.phone/email are unique as well.
     * - customers.user_id should point to created user.
     */
    @Transactional
    public CreatedCustomer createCustomer(CashierCustomerCreateRequest request) {
        String phone = normalize(request.getPhone());
        String email = normalizeEmail(request.getEmail());

        // Uniqueness checks across BOTH tables (users + customers)
        if (phone != null && customerRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Customer phone already exists: " + phone);
        }
        if (email != null && customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Customer email already exists: " + email);
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User email already exists: " + email);
        }

        // 1) Create user row (role CUSTOMER)
        User user = new User();
        user.setFirstName(trimToNull(request.getFirstName()));
        user.setLastName(trimToNull(request.getLastName()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(null);
        user.setRole(User.UserRole.CUSTOMER);
        user.setIsActive(true);

        // Cashier-created customer doesn't set a real password. Generate random.
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));

        User savedUser = userRepository.save(user);

        // 2) Create customer row linked to user
        Customer c = new Customer();
        c.setFirstName(savedUser.getFirstName());
        c.setLastName(savedUser.getLastName());
        c.setPhone(savedUser.getPhone());
        c.setEmail(savedUser.getEmail());

        if (request.getGender() != null && !request.getGender().isBlank()) {
            c.setGender(request.getGender().trim().toUpperCase().charAt(0));
        }
        c.setBirthDate(request.getBirthDate());
        c.setLastVisitedAt(null);
        c.setUserId(savedUser.getId());
        c.setUpdatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(c);

        return new CreatedCustomer(savedUser, savedCustomer);
    }

    @Transactional(readOnly = true)
    public Customer getById(Integer id) {
        return customerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
    }

    private static String normalize(String s) {
        return trimToNull(s);
    }

    private static String normalizeEmail(String s) {
        String v = trimToNull(s);
        return v == null ? null : v.toLowerCase();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public record CreatedCustomer(User user, Customer customer) {}
}

package com.example.back_end.modules.register.service;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.mapper.UserMapper;
import com.example.back_end.modules.register.repository.CustomerRepository;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException("Passwords do not match");
        }

        // Check if email already exists (users table)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered");
        }
        // Check if phone/email already exists (customers table) to avoid duplicates
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered as customer");
        }
        if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone())) {
            throw new CustomException("Phone already registered as customer");
        }

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Self registration (first CEO or CUSTOMER)
        if (request.getIsSelfRegistration() != null && request.getIsSelfRegistration()) {
            if (!request.getRole().equals(UserRole.CEO) && !request.getRole().equals(UserRole.CUSTOMER)) {
                throw new CustomException("Self registration is only allowed for CEO or CUSTOMER roles");
            }
            return createUserAndCustomerIfNeeded(request);
        }

        // If no authenticated user
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new CustomException("You must be logged in to create a user account");
        }

        // Get current user
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new CustomException("Current user not found"));

        // Validate permissions
        validateRolePermissions(currentUser.getRole(), request.getRole());

        // Create the user (and customer if CUSTOMER role)
        return createUserAndCustomerIfNeeded(request);
    }

    private void validateRolePermissions(UserRole currentUserRole, UserRole targetRole) {

        // CEO can create: STORE_MANAGER, INVENTORY_MANAGER, CASHIER
        if (currentUserRole.equals(UserRole.CEO)) {
            List<UserRole> allowedRoles = Arrays.asList(
                    UserRole.STORE_MANAGER,
                    UserRole.INVENTORY_MANAGER,
                    UserRole.CASHIER
            );
            if (!allowedRoles.contains(targetRole)) {
                throw new CustomException("CEO can only create Store Manager, Inventory Manager, or Cashier accounts");
            }
            return;
        }

        // STORE_MANAGER can only create: CASHIER
        if (currentUserRole.equals(UserRole.STORE_MANAGER)) {
            if (!targetRole.equals(UserRole.CASHIER)) {
                throw new CustomException("Store Manager can only create Cashier accounts");
            }
            return;
        }

        // INVENTORY_MANAGER and CASHIER cannot create any users
        throw new CustomException("You don't have permission to create user accounts");
    }

    private RegisterResponseDTO createUserAndCustomerIfNeeded(RegisterRequestDTO request) {
        // 1) Create new user
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // 2) If role is CUSTOMER, also create a record in customers table
        if (UserRole.CUSTOMER.equals(savedUser.getRole())) {
            Customer customer = new Customer();
            customer.setFirstName(savedUser.getFirstName());
            customer.setLastName(savedUser.getLastName());
            customer.setPhone(savedUser.getPhone());
            customer.setEmail(savedUser.getEmail());
            customer.setLastVisitedAt(LocalDateTime.now());
            customer.setUserId(savedUser.getId());
            customerRepository.save(customer);
        }

        // 3) Generate JWT token
        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );

        // 4) Return response
        return userMapper.toRegisterResponseDTO(savedUser, token);
    }
}
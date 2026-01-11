package com.example.back_end.modules.register.controller;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.register.dto.UserDTO;
import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.dto.UpdateUserRequestDTO;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegisterController {

    private final RegisterService registerService;
    private final UserRepository userRepository;

    /**
     * Self registration for the first CEO only.
     */
    @PostMapping("/register/ceo")
    public ResponseEntity<RegisterResponseDTO> registerCEO(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        request.setIsSelfRegistration(true);
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CEO / Store Manager register other users (Store Manager / Cashier).
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<RegisterResponseDTO> registerUser(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        request.setIsSelfRegistration(false);
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public customer self registration.
     * Anonymous users can create a CUSTOMER account.
     */
    @PostMapping("/register/customer")
    public ResponseEntity<RegisterResponseDTO> registerCustomer(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        request.setIsSelfRegistration(true);
        // Force role to CUSTOMER to prevent privilege escalation
        request.setRole(UserRole.CUSTOMER);
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Get all users (CEO only).
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<UserDTO> dtos = users.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .isActive(user.getIsActive())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build()
                )
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Update user data (CEO only).
     * - CEO can update: firstName, lastName, email, phone, address, role, isActive
     * - Password is NOT updated here for security reasons.
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Integer id,
            @RequestBody UpdateUserRequestDTO request
    ) {
        // 1) Find user by id
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found"));

        // 2) Update only non-null fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        // 3) Save changes to DB
        User savedUser = userRepository.save(user);

        // 4) Map to DTO
        UserDTO dto = UserDTO.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .role(savedUser.getRole())
                .isActive(savedUser.getIsActive())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();

        return ResponseEntity.ok(dto);
    }
}

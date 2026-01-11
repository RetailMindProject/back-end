package com.example.back_end.modules.register.service;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.mapper.UserMapper;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered");
        }

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // إذا كان التسجيل الذاتي (للـ CEO الأول أو CUSTOMER)
        if (request.getIsSelfRegistration() != null && request.getIsSelfRegistration()) {
            if (!request.getRole().equals(UserRole.CEO) && !request.getRole().equals(UserRole.CUSTOMER)) {
                throw new CustomException("Self registration is only allowed for CEO or CUSTOMER roles");
            }
            return createUser(request);
        }

        // إذا لم يكن هناك مستخدم مسجل دخول
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new CustomException("You must be logged in to create a user account");
        }

        // الحصول على المستخدم الحالي
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new CustomException("Current user not found"));

        // التحقق من الصلاحيات
        validateRolePermissions(currentUser.getRole(), request.getRole());

        // Create the user
        return createUser(request);
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

    private RegisterResponseDTO createUser(RegisterRequestDTO request) {
        // Create new user
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token with user details for introspection
        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );

        // Return response
        return userMapper.toRegisterResponseDTO(savedUser, token);
    }
}
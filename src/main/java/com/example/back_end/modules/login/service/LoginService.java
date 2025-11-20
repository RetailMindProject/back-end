package com.example.back_end.modules.login.service;

import com.example.back_end.modules.login.dto.LoginRequestDTO;
import com.example.back_end.modules.login.dto.LoginResponseDTO;
import com.example.back_end.modules.login.entity.uesr;
import com.example.back_end.modules.login.reporsitory.repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

    private final repository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            // البحث عن المستخدم بالإيميل والتأكد من أنه نشط
            uesr user = userRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            // التحقق من كلمة المرور
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("Failed login attempt for email: {}", loginRequest.getEmail());
                throw new RuntimeException("Invalid email or password");
            }

            // تحديد URL للتوجيه بناءً على الدور
            String redirectUrl = getRedirectUrlByRole(user.getRole());

            // يمكنك هنا إنشاء JWT token إذا كنت تستخدم JWT
            String token = generateToken(user);

            log.info("Successful login for user: {} with role: {}", user.getEmail(), user.getRole());

            return LoginResponseDTO.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .token(token)
                    .redirectUrl(redirectUrl)
                    .message("Login successful")
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return LoginResponseDTO.builder()
                    .message(e.getMessage())
                    .success(false)
                    .build();
        }
    }

    private String getRedirectUrlByRole(String role) {
        return switch (role) {
            case "CEO" -> "/dashboard/ceo";
            case "STORE_MANAGER" -> "/dashboard/store-manager";
            case "INVENTORY_MANAGER" -> "/dashboard/inventory-manager";
            case "CASHIER" -> "/dashboard/cashier";
            default -> "/dashboard";
        };
    }

    private String generateToken(uesr user) {
        // هنا يمكنك إضافة منطق إنشاء JWT token
        // مؤقتاً سنرجع token بسيط
        return "Bearer_token_for_" + user.getEmail();
    }
}
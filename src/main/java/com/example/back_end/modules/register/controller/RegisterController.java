package com.example.back_end.modules.register.controller;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.register.dto.UserDTO;
import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegisterController {

    private final RegisterService registerService;
    private final UserRepository userRepository;   // ✅ أضفنا الـ repository

    /**
     * التسجيل الذاتي للـ CEO الأول فقط (بدون authentication)
     */
    @PostMapping("/register/ceo")
    public ResponseEntity<RegisterResponseDTO> registerCEO(@Valid @RequestBody RegisterRequestDTO request) {
        request.setIsSelfRegistration(true);
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * إنشاء مستخدم جديد من قبل CEO أو Store Manager
     * يتطلب تسجيل دخول وصلاحيات مناسبة
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<RegisterResponseDTO> registerUser(@Valid @RequestBody RegisterRequestDTO request) {
        request.setIsSelfRegistration(false);
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * جلب معلومات المستخدم الحالي (من الـ JWT)
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        // الإيميل جاي من الـ JWT
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())        // تأكد أن عندك الحقول phone و address في User
                .address(user.getAddress())
                .build();

        return ResponseEntity.ok(dto);
    }
}

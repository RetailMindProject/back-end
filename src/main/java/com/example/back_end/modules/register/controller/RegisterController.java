package com.example.back_end.modules.register.controller;

import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegisterController {

    private final RegisterService registerService;

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
}
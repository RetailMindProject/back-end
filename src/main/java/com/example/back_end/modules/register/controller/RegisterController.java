package com.example.back_end.modules.register.controller;

import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
package com.example.back_end.modules.login.controller;

import com.example.back_end.modules.login.entity.uesr;
import com.example.back_end.modules.login.reporsitory.repository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final repository userRepository;
    private final PasswordEncoder passwordEncoder;

    // عرض جميع المستخدمين
    @GetMapping("/users")
    public List<uesr> getAllUsers() {
        return userRepository.findAll();
    }

    // إنشاء user جديد للاختبار
    @PostMapping("/create-user")
    public uesr createTestUser(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String role) {
        uesr user = new uesr();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    // حذف user
    @DeleteMapping("/delete-user/{id}")
    public String deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
        return "User deleted successfully";
    }

    // تشفير كلمة مرور للاختبار
    @GetMapping("/encode-password")
    public String encodePassword(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}
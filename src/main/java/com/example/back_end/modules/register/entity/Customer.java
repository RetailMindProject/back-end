package com.example.back_end.modules.register.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", length = 60)
    private String firstName;

    @Column(name = "last_name", length = 60)
    private String lastName;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(length = 120, unique = true)
    private String email;

    /** gender is optional; database allows M/F/null */
    @Column(length = 1)
    private Character gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "last_visited_at")
    private LocalDateTime lastVisitedAt;

    @Column(name = "user_id")
    private Integer userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


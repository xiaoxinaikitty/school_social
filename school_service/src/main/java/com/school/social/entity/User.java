package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String school;
    private String college;
    private String grade;
    private String bio;
    private Integer status;
    private String registerIp;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

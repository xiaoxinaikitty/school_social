package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCode {
    private Long id;
    private Long userId;
    private String email;
    private String scene;
    private String codeHash;
    private LocalDateTime expireAt;
    private LocalDateTime usedAt;
    private Integer status;
    private Integer attemptCount;
    private String requestIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private Integer targetType;
    private Long targetId;
    private Long auditorId;
    private Integer decision;
    private String note;
    private LocalDateTime createdAt;
}

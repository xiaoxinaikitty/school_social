package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Like {
    private Long id;
    private Long userId;
    private Integer targetType;
    private Long targetId;
    private LocalDateTime createdAt;
}

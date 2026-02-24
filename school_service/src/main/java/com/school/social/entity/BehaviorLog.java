package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorLog {
    private Long id;
    private Long userId;
    private Integer actionType;
    private Integer targetType;
    private Long targetId;
    private Integer durationSec;
    private BigDecimal weight;
    private LocalDateTime createdAt;
}

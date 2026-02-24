package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTag {
    private Long userId;
    private Long tagId;
    private BigDecimal weight;
    private LocalDateTime updatedAt;
}

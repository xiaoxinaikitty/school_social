package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationLog {
    private Long id;
    private Long userId;
    private Long postId;
    private Integer scene;
    private BigDecimal score;
    private Integer rankPos;
    private Integer isClicked;
    private LocalDateTime createdAt;
}

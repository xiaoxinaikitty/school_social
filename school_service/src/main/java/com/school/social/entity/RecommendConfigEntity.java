package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendConfigEntity {
    private Long id;
    private Integer enableHot;
    private Integer enableFollow;
    private Integer enableTag;
    private Double weightHot;
    private Double weightTime;
    private Double weightQuality;
    private Double weightTag;
    private Double weightFollow;
    private LocalDateTime updatedAt;
}

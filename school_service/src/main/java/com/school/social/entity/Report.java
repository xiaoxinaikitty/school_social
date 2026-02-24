package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private Long id;
    private Long reporterId;
    private Integer targetType;
    private Long targetId;
    private String reason;
    private String detail;
    private Integer status;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String result;
    private LocalDateTime createdAt;
}

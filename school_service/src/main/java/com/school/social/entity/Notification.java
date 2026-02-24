package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private Integer refType;
    private Long refId;
    private Integer isRead;
    private LocalDateTime createdAt;
}

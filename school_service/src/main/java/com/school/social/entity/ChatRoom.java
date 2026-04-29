package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private Long id;
    private String name;
    private String avatarUrl;
    private String description;
    private Long ownerId;
    private Integer roomType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;
    private Integer messageType;
    private Integer status;
    private LocalDateTime createdAt;
}

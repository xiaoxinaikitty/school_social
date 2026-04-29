package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    private Long id;
    private Long userOneId;
    private Long userTwoId;
    private Long directRoomId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

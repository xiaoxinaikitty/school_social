package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember {
    private Long id;
    private Long roomId;
    private Long userId;
    private Integer roleType;
    private LocalDateTime muteUntil;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
}

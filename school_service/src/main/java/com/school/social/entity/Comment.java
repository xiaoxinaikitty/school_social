package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer status;
    private Integer likeCount;
    private LocalDateTime createdAt;
}

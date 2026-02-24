package com.school.social.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMedia {
    private Long id;
    private Long postId;
    private Integer mediaType;
    private String url;
    private Integer sortOrder;
    private Integer width;
    private Integer height;
    private Integer durationSec;
    private LocalDateTime createdAt;
}

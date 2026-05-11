package com.school.social.service;

public interface ContentDeletionService {
    int deleteComment(Long commentId);

    boolean deletePost(Long postId);
}

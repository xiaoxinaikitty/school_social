package com.school.social.service;

import com.school.social.common.PageResponse;
import com.school.social.dto.admin.RecommendOverview;
import com.school.social.entity.Post;

public interface RecommendationService {
    PageResponse<Post> recommend(Long userId, int page, int size);
    void recordClick(Long userId, Long postId, Integer scene);
    void recordDetailView(Long userId, Long postId);
    RecommendOverview getOverview(int days);
}

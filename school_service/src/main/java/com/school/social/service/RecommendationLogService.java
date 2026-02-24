package com.school.social.service;

import java.util.List;
import com.school.social.entity.RecommendationLog;

public interface RecommendationLogService {
    int insert(RecommendationLog entity);
    int updateById(RecommendationLog entity);
    int deleteById(Long id);
    RecommendationLog selectById(Long id);
    List<RecommendationLog> selectAll();
}

package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import java.time.LocalDateTime;

import com.school.social.dto.admin.RecommendPostStat;
import com.school.social.dto.admin.RecommendSceneStat;
import com.school.social.entity.RecommendationLog;

@Mapper
public interface RecommendationLogMapper {
    int insert(RecommendationLog entity);
    int updateById(RecommendationLog entity);
    int deleteById(Long id);
    RecommendationLog selectById(Long id);
    List<RecommendationLog> selectAll();

    RecommendationLog selectLatestByUserAndPostScene(@Param("userId") Long userId,
                                                     @Param("postId") Long postId,
                                                     @Param("scene") Integer scene);

    int countSince(@Param("start") LocalDateTime start);

    int countClicksSince(@Param("start") LocalDateTime start);

    List<RecommendSceneStat> selectSceneStatsSince(@Param("start") LocalDateTime start);

    List<RecommendPostStat> selectTopPostsSince(@Param("start") LocalDateTime start,
                                                @Param("limit") int limit);
}

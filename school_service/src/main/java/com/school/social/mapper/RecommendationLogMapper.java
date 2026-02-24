package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.RecommendationLog;

@Mapper
public interface RecommendationLogMapper {
    int insert(RecommendationLog entity);
    int updateById(RecommendationLog entity);
    int deleteById(Long id);
    RecommendationLog selectById(Long id);
    List<RecommendationLog> selectAll();
}

package com.school.social.mapper;

import com.school.social.entity.RecommendConfigEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecommendConfigMapper {
    RecommendConfigEntity selectById(Long id);
    int insert(RecommendConfigEntity entity);
    int updateById(RecommendConfigEntity entity);
}

package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Like;

@Mapper
public interface LikeMapper {
    int insert(Like entity);
    int updateById(Like entity);
    int deleteById(Long id);
    Like selectById(Long id);
    List<Like> selectAll();

    Like selectByUserAndTarget(@Param("userId") Long userId,
                               @Param("targetType") Integer targetType,
                               @Param("targetId") Long targetId);

    int deleteByUserAndTarget(@Param("userId") Long userId,
                              @Param("targetType") Integer targetType,
                              @Param("targetId") Long targetId);
}

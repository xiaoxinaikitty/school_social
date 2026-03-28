package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.school.social.entity.BehaviorLog;

@Mapper
public interface BehaviorLogMapper {
    int insert(BehaviorLog entity);
    int updateById(BehaviorLog entity);
    int deleteById(Long id);
    BehaviorLog selectById(Long id);
    List<BehaviorLog> selectAll();

    List<BehaviorLog> selectRecentByUser(@Param("userId") Long userId,
                                         @Param("limit") int limit);
}

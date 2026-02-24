package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.BehaviorLog;

@Mapper
public interface BehaviorLogMapper {
    int insert(BehaviorLog entity);
    int updateById(BehaviorLog entity);
    int deleteById(Long id);
    BehaviorLog selectById(Long id);
    List<BehaviorLog> selectAll();
}

package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Follow;

@Mapper
public interface FollowMapper {
    int insert(Follow entity);
    int updateById(Follow entity);
    int deleteById(Long id);
    Follow selectById(Long id);
    List<Follow> selectAll();
}

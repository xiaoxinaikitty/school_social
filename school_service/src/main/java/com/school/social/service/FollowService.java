package com.school.social.service;

import java.util.List;
import com.school.social.entity.Follow;

public interface FollowService {
    int insert(Follow entity);
    int updateById(Follow entity);
    int deleteById(Long id);
    Follow selectById(Long id);
    List<Follow> selectAll();
}

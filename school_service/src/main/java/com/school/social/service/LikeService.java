package com.school.social.service;

import java.util.List;
import com.school.social.entity.Like;

public interface LikeService {
    int insert(Like entity);
    int updateById(Like entity);
    int deleteById(Long id);
    Like selectById(Long id);
    List<Like> selectAll();
}

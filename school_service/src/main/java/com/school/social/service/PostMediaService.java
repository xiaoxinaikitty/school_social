package com.school.social.service;

import java.util.List;
import com.school.social.entity.PostMedia;

public interface PostMediaService {
    int insert(PostMedia entity);
    int updateById(PostMedia entity);
    int deleteById(Long id);
    PostMedia selectById(Long id);
    List<PostMedia> selectAll();
}

package com.school.social.service;

import java.util.List;
import com.school.social.entity.Post;

public interface PostService {
    int insert(Post entity);
    int updateById(Post entity);
    int deleteById(Long id);
    Post selectById(Long id);
    List<Post> selectAll();
}

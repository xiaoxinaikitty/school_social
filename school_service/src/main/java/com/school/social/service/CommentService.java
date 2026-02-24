package com.school.social.service;

import java.util.List;
import com.school.social.entity.Comment;

public interface CommentService {
    int insert(Comment entity);
    int updateById(Comment entity);
    int deleteById(Long id);
    Comment selectById(Long id);
    List<Comment> selectAll();
}

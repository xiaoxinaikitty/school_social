package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Comment;

@Mapper
public interface CommentMapper {
    int insert(Comment entity);
    int updateById(Comment entity);
    int deleteById(Long id);
    Comment selectById(Long id);
    List<Comment> selectAll();
}

package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Comment;

@Mapper
public interface CommentMapper {
    int insert(Comment entity);
    int updateById(Comment entity);
    int deleteById(Long id);
    Comment selectById(Long id);
    List<Comment> selectAll();

    List<Comment> selectByUserId(@Param("userId") Long userId);

    List<Comment> selectByUserIdPaged(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    int countByUserId(@Param("userId") Long userId);
}

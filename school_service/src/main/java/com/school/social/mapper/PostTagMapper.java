package com.school.social.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.PostTag;

@Mapper
public interface PostTagMapper {
    int insert(PostTag entity);
    int deleteByPk(@Param("postId") Long postId, @Param("tagId") Long tagId);
    PostTag selectByPk(@Param("postId") Long postId, @Param("tagId") Long tagId);
    List<PostTag> selectAll();
}

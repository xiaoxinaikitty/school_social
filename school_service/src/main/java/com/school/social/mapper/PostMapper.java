package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Post;

@Mapper
public interface PostMapper {
    int insert(Post entity);
    int updateById(Post entity);
    int deleteById(Long id);
    Post selectById(Long id);
    List<Post> selectAll();
}

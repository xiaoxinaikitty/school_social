package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.PostMedia;

@Mapper
public interface PostMediaMapper {
    int insert(PostMedia entity);
    int updateById(PostMedia entity);
    int deleteById(Long id);
    PostMedia selectById(Long id);
    List<PostMedia> selectAll();
}

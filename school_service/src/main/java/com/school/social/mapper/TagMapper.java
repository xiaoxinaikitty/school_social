package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Tag;

@Mapper
public interface TagMapper {
    int insert(Tag entity);
    int updateById(Tag entity);
    int deleteById(Long id);
    Tag selectById(Long id);
    List<Tag> selectAll();
}

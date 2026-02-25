package com.school.social.mapper;

import com.school.social.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {
    int insert(Tag entity);
    int updateById(Tag entity);
    int deleteById(Long id);
    Tag selectById(Long id);
    List<Tag> selectAll();

    List<Long> selectExistingIds(@Param("ids") List<Long> ids);
}

package com.school.social.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.UserTag;

@Mapper
public interface UserTagMapper {
    int insert(UserTag entity);
    int deleteByPk(@Param("userId") Long userId, @Param("tagId") Long tagId);
    UserTag selectByPk(@Param("userId") Long userId, @Param("tagId") Long tagId);
    List<UserTag> selectAll();
}

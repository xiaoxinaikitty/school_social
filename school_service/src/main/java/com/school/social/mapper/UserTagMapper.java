package com.school.social.mapper;

import com.school.social.dto.user.UserTagView;
import com.school.social.entity.UserTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTagMapper {
    int insert(UserTag entity);
    int deleteByPk(@Param("userId") Long userId, @Param("tagId") Long tagId);
    UserTag selectByPk(@Param("userId") Long userId, @Param("tagId") Long tagId);
    List<UserTag> selectAll();

    int deleteByUserId(@Param("userId") Long userId);
    List<UserTagView> selectByUserId(@Param("userId") Long userId);
}

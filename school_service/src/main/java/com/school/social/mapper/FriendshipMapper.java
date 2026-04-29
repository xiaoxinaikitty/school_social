package com.school.social.mapper;

import com.school.social.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendshipMapper {
    int insert(Friendship entity);

    int updateById(Friendship entity);

    Friendship selectById(Long id);

    Friendship selectByUsers(@Param("userOneId") Long userOneId, @Param("userTwoId") Long userTwoId);
}

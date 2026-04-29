package com.school.social.mapper;

import com.school.social.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendRequestMapper {
    int insert(FriendRequest entity);

    int updateById(FriendRequest entity);

    FriendRequest selectById(Long id);

    FriendRequest selectPendingBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);
}

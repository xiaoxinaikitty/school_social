package com.school.social.mapper;

import com.school.social.entity.ChatRoomMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRoomMemberMapper {
    int insert(ChatRoomMember entity);

    int deleteByRoomIdAndUserId(@Param("roomId") Long roomId,
                                @Param("userId") Long userId);

    ChatRoomMember selectByRoomIdAndUserId(@Param("roomId") Long roomId,
                                           @Param("userId") Long userId);

    int updateLastReadMessageId(@Param("roomId") Long roomId,
                                @Param("userId") Long userId,
                                @Param("lastReadMessageId") Long lastReadMessageId);
}

package com.school.social.mapper;

import com.school.social.dto.chat.ChatMessageView;
import com.school.social.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    int insert(ChatMessage entity);

    ChatMessage selectById(Long id);

    ChatMessageView selectMessageViewById(Long id);

    List<ChatMessageView> selectMessageViewsByRoomId(@Param("roomId") Long roomId,
                                                     @Param("offset") int offset,
                                                     @Param("size") int size);

    int countByRoomId(@Param("roomId") Long roomId);

    Long selectLatestMessageIdByRoomId(@Param("roomId") Long roomId);
}

package com.school.social.mapper;

import com.school.social.dto.chat.ChatRoomView;
import com.school.social.entity.ChatRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRoomMapper {
    int insert(ChatRoom entity);

    int updateById(ChatRoom entity);

    ChatRoom selectById(Long id);

    int countAll();

    List<ChatRoomView> selectRoomViews(@Param("userId") Long userId);
}

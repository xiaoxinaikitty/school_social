package com.school.social.service;

import com.school.social.common.PageResponse;
import com.school.social.dto.chat.ChatMessageView;
import com.school.social.dto.chat.ChatRoomCreateRequest;
import com.school.social.dto.chat.ChatRoomView;

import java.util.List;

public interface ChatService {
    List<ChatRoomView> listRooms(Long userId);

    ChatRoomView createRoom(Long userId, ChatRoomCreateRequest request);

    void joinRoom(Long userId, Long roomId);

    void quitRoom(Long userId, Long roomId);

    PageResponse<ChatMessageView> listMessages(Long userId, Long roomId, int page, int size);

    void markRoomRead(Long userId, Long roomId);

    boolean isMember(Long userId, Long roomId);

    ChatMessageView saveMessage(Long userId, Long roomId, String content, Integer messageType);
}

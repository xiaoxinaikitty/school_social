package com.school.social.service.impl;

import com.school.social.common.PageResponse;
import com.school.social.dto.chat.ChatMessageView;
import com.school.social.dto.chat.ChatRoomCreateRequest;
import com.school.social.dto.chat.ChatRoomView;
import com.school.social.entity.ChatMessage;
import com.school.social.entity.ChatRoom;
import com.school.social.entity.ChatRoomMember;
import com.school.social.mapper.ChatMessageMapper;
import com.school.social.mapper.ChatRoomMapper;
import com.school.social.mapper.ChatRoomMemberMapper;
import com.school.social.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {
    private static final int ROOM_TYPE_PUBLIC = 0;
    private static final int ROOM_TYPE_DIRECT = 1;
    private static final int ROOM_STATUS_NORMAL = 0;
    private static final int MEMBER_ROLE_NORMAL = 0;
    private static final int MEMBER_ROLE_OWNER = 2;
    private static final int MESSAGE_TYPE_TEXT = 0;
    private static final int MESSAGE_STATUS_NORMAL = 0;
    private static final int MAX_MESSAGE_LENGTH = 500;

    @Resource
    private ChatRoomMapper chatRoomMapper;

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatRoomView> listRooms(Long userId) {
        ensureDefaultRoom(userId);
        return chatRoomMapper.selectRoomViews(userId);
    }

    @Override
    public ChatRoomView createRoom(Long userId, ChatRoomCreateRequest request) {
        String name = request == null || request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("群聊名称不能为空");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("群聊名称不能超过 50 个字符");
        }
        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (description.length() > 255) {
            throw new IllegalArgumentException("群聊简介不能超过 255 个字符");
        }

        LocalDateTime now = LocalDateTime.now();
        ChatRoom room = new ChatRoom();
        room.setName(name);
        room.setDescription(description.isEmpty() ? null : description);
        room.setOwnerId(userId);
        room.setRoomType(ROOM_TYPE_PUBLIC);
        room.setStatus(ROOM_STATUS_NORMAL);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        chatRoomMapper.insert(room);

        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRoleType(MEMBER_ROLE_OWNER);
        member.setJoinedAt(now);
        chatRoomMemberMapper.insert(member);

        return findRoomView(userId, room.getId());
    }

    @Override
    public void joinRoom(Long userId, Long roomId) {
        ChatRoom room = requireRoom(roomId);
        if (room.getRoomType() != null && room.getRoomType() == ROOM_TYPE_DIRECT) {
            throw new IllegalArgumentException("好友私聊不支持手动加入");
        }
        if (!isRoomStatusNormal(room.getStatus())) {
            throw new IllegalArgumentException("群聊不可加入");
        }
        ChatRoomMember existing = chatRoomMemberMapper.selectByRoomIdAndUserId(roomId, userId);
        if (existing != null) {
            return;
        }
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRoleType(MEMBER_ROLE_NORMAL);
        member.setJoinedAt(LocalDateTime.now());
        chatRoomMemberMapper.insert(member);
    }

    @Override
    public void quitRoom(Long userId, Long roomId) {
        ChatRoom room = requireRoom(roomId);
        if (room.getRoomType() != null && room.getRoomType() == ROOM_TYPE_DIRECT) {
            throw new IllegalArgumentException("好友私聊不支持退出");
        }
        if (room.getOwnerId() != null && room.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("群主暂不支持退出群聊");
        }
        chatRoomMemberMapper.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Override
    public PageResponse<ChatMessageView> listMessages(Long userId, Long roomId, int page, int size) {
        assertMember(userId, roomId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<ChatMessageView> list = chatMessageMapper.selectMessageViewsByRoomId(roomId, offset, safeSize);
        long total = chatMessageMapper.countByRoomId(roomId);
        return new PageResponse<>(safePage, safeSize, total, list);
    }

    @Override
    public void markRoomRead(Long userId, Long roomId) {
        assertMember(userId, roomId);
        Long latestMessageId = chatMessageMapper.selectLatestMessageIdByRoomId(roomId);
        if (latestMessageId == null) {
            return;
        }
        chatRoomMemberMapper.updateLastReadMessageId(roomId, userId, latestMessageId);
    }

    @Override
    public boolean isMember(Long userId, Long roomId) {
        if (userId == null || roomId == null) {
            return false;
        }
        return chatRoomMemberMapper.selectByRoomIdAndUserId(roomId, userId) != null;
    }

    @Override
    public ChatMessageView saveMessage(Long userId, Long roomId, String content) {
        assertMember(userId, roomId);
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (safeContent.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息长度不能超过 500 个字符");
        }
        ChatRoomMember member = chatRoomMemberMapper.selectByRoomIdAndUserId(roomId, userId);
        if (member != null && member.getMuteUntil() != null && member.getMuteUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("你当前已被禁言");
        }
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(userId);
        message.setContent(safeContent);
        message.setMessageType(MESSAGE_TYPE_TEXT);
        message.setStatus(MESSAGE_STATUS_NORMAL);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
        chatRoomMemberMapper.updateLastReadMessageId(roomId, userId, message.getId());
        return chatMessageMapper.selectMessageViewById(message.getId());
    }

    private void ensureDefaultRoom(Long userId) {
        if (userId == null || chatRoomMapper.countAll() > 0) {
            return;
        }
        ChatRoom room = new ChatRoom();
        LocalDateTime now = LocalDateTime.now();
        room.setName("校园大厅");
        room.setDescription("默认公共群，欢迎交流校园动态与日常话题。");
        room.setOwnerId(userId);
        room.setRoomType(ROOM_TYPE_PUBLIC);
        room.setStatus(ROOM_STATUS_NORMAL);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        chatRoomMapper.insert(room);

        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRoleType(MEMBER_ROLE_OWNER);
        member.setJoinedAt(now);
        chatRoomMemberMapper.insert(member);
    }

    private ChatRoomView findRoomView(Long userId, Long roomId) {
        List<ChatRoomView> rooms = chatRoomMapper.selectRoomViews(userId);
        for (ChatRoomView room : rooms) {
            if (roomId.equals(room.getId())) {
                return room;
            }
        }
        return null;
    }

    private ChatRoom requireRoom(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("群聊不存在");
        }
        return room;
    }

    private void assertMember(Long userId, Long roomId) {
        requireRoom(roomId);
        if (!isMember(userId, roomId)) {
            throw new IllegalArgumentException("请先加入群聊");
        }
    }

    private boolean isRoomStatusNormal(Integer status) {
        return status != null && status == ROOM_STATUS_NORMAL;
    }
}

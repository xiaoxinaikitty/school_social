package com.school.social.service.impl;

import com.school.social.common.PageResponse;
import com.school.social.dto.auth.UserView;
import com.school.social.dto.friend.FriendCandidateView;
import com.school.social.dto.friend.FriendRequestCreateRequest;
import com.school.social.dto.friend.FriendRequestRespondRequest;
import com.school.social.entity.ChatRoom;
import com.school.social.entity.ChatRoomMember;
import com.school.social.entity.FriendRequest;
import com.school.social.entity.Friendship;
import com.school.social.entity.Notification;
import com.school.social.entity.User;
import com.school.social.mapper.ChatRoomMapper;
import com.school.social.mapper.ChatRoomMemberMapper;
import com.school.social.mapper.FriendRequestMapper;
import com.school.social.mapper.FriendshipMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.UserMapper;
import com.school.social.service.FriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FriendServiceImpl implements FriendService {
    private static final int REQUEST_STATUS_PENDING = 0;
    private static final int REQUEST_STATUS_ACCEPTED = 1;
    private static final int REQUEST_STATUS_REJECTED = 2;

    private static final int ROOM_TYPE_DIRECT = 1;
    private static final int ROOM_STATUS_NORMAL = 0;
    private static final int MEMBER_ROLE_NORMAL = 0;

    private static final int NOTIFY_FRIEND_REQUEST = 4;
    private static final int NOTIFY_FRIEND_RESULT = 5;
    private static final int REF_CHAT = 3;
    private static final int REF_FRIEND_REQUEST = 4;
    private static final int REF_USER = 2;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FriendRequestMapper friendRequestMapper;

    @Resource
    private FriendshipMapper friendshipMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private ChatRoomMapper chatRoomMapper;

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Override
    public PageResponse<FriendCandidateView> searchCandidates(Long userId, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 20);
        int offset = (safePage - 1) * safeSize;
        List<UserView> users = userMapper.searchUserViews(keyword, userId, offset, safeSize);
        long total = userMapper.countSearchUserViews(keyword, userId);
        List<FriendCandidateView> list = new ArrayList<>();
        for (UserView user : users) {
            FriendCandidateView item = new FriendCandidateView();
            item.setId(user.getId());
            item.setUsername(user.getUsername());
            item.setAvatarUrl(user.getAvatarUrl());
            item.setSchool(user.getSchool());
            item.setCollege(user.getCollege());
            item.setBio(user.getBio());

            Friendship friendship = friendshipMapper.selectByUsers(normalizeUserOne(userId, user.getId()), normalizeUserTwo(userId, user.getId()));
            if (friendship != null) {
                item.setRelationStatus("friend");
                item.setDirectRoomId(friendship.getDirectRoomId());
            } else {
                FriendRequest pending = friendRequestMapper.selectPendingBetweenUsers(userId, user.getId());
                if (pending == null) {
                    item.setRelationStatus("none");
                } else if (userId.equals(pending.getRequesterId())) {
                    item.setRelationStatus("request_sent");
                    item.setPendingRequestId(pending.getId());
                } else {
                    item.setRelationStatus("request_received");
                    item.setPendingRequestId(pending.getId());
                }
            }
            list.add(item);
        }
        return new PageResponse<>(safePage, safeSize, total, list);
    }

    @Override
    public void sendRequest(Long userId, FriendRequestCreateRequest request) {
        Long targetUserId = request == null ? null : request.getTargetUserId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("目标用户不能为空");
        }
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }

        Friendship friendship = friendshipMapper.selectByUsers(normalizeUserOne(userId, targetUserId), normalizeUserTwo(userId, targetUserId));
        if (friendship != null) {
            throw new IllegalArgumentException("你们已经是好友了");
        }

        FriendRequest pending = friendRequestMapper.selectPendingBetweenUsers(userId, targetUserId);
        if (pending != null) {
            if (userId.equals(pending.getRequesterId())) {
                throw new IllegalArgumentException("好友申请已发送，请等待对方处理");
            }
            throw new IllegalArgumentException("对方已向你发送好友申请，请在通知中处理");
        }

        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.length() > 200) {
            throw new IllegalArgumentException("申请附言不能超过 200 个字符");
        }

        LocalDateTime now = LocalDateTime.now();
        FriendRequest entity = new FriendRequest();
        entity.setRequesterId(userId);
        entity.setReceiverId(targetUserId);
        entity.setRequestMessage(message.isEmpty() ? null : message);
        entity.setStatus(REQUEST_STATUS_PENDING);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        friendRequestMapper.insert(entity);

        User requester = userMapper.selectById(userId);
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setType(NOTIFY_FRIEND_REQUEST);
        notification.setTitle("新的好友申请");
        notification.setContent(buildRequestContent(requester == null ? null : requester.getUsername(), message));
        notification.setRefType(REF_FRIEND_REQUEST);
        notification.setRefId(entity.getId());
        notification.setIsRead(0);
        notification.setCreatedAt(now);
        notificationMapper.insert(notification);
    }

    @Override
    public void respondRequest(Long userId, Long requestId, FriendRequestRespondRequest request) {
        FriendRequest entity = friendRequestMapper.selectById(requestId);
        if (entity == null) {
            throw new IllegalArgumentException("好友申请不存在");
        }
        if (!userId.equals(entity.getReceiverId())) {
            throw new IllegalArgumentException("无权处理这条好友申请");
        }
        if (entity.getStatus() == null || entity.getStatus() != REQUEST_STATUS_PENDING) {
            throw new IllegalArgumentException("这条好友申请已经处理过了");
        }
        boolean approved = request != null && Boolean.TRUE.equals(request.getApproved());
        LocalDateTime now = LocalDateTime.now();

        FriendRequest update = new FriendRequest();
        update.setId(entity.getId());
        update.setStatus(approved ? REQUEST_STATUS_ACCEPTED : REQUEST_STATUS_REJECTED);
        update.setProcessedAt(now);
        update.setUpdatedAt(now);
        friendRequestMapper.updateById(update);

        User requester = userMapper.selectById(entity.getRequesterId());
        User receiver = userMapper.selectById(entity.getReceiverId());
        if (approved) {
            Friendship friendship = friendshipMapper.selectByUsers(
                normalizeUserOne(entity.getRequesterId(), entity.getReceiverId()),
                normalizeUserTwo(entity.getRequesterId(), entity.getReceiverId())
            );
            if (friendship == null) {
                friendship = new Friendship();
                friendship.setUserOneId(normalizeUserOne(entity.getRequesterId(), entity.getReceiverId()));
                friendship.setUserTwoId(normalizeUserTwo(entity.getRequesterId(), entity.getReceiverId()));
                friendship.setCreatedAt(now);
                friendship.setUpdatedAt(now);
                friendshipMapper.insert(friendship);
            }
            if (friendship.getDirectRoomId() == null) {
                Long roomId = createDirectRoom(entity.getRequesterId(), entity.getReceiverId(), now);
                Friendship friendshipUpdate = new Friendship();
                friendshipUpdate.setId(friendship.getId());
                friendshipUpdate.setDirectRoomId(roomId);
                friendshipUpdate.setUpdatedAt(now);
                friendshipMapper.updateById(friendshipUpdate);
                friendship.setDirectRoomId(roomId);
            }
            Notification notification = new Notification();
            notification.setUserId(entity.getRequesterId());
            notification.setType(NOTIFY_FRIEND_RESULT);
            notification.setTitle("好友申请已通过");
            notification.setContent((receiver == null ? "对方" : receiver.getUsername()) + " 已通过你的好友申请，现在可以开始私聊了。");
            notification.setRefType(REF_CHAT);
            notification.setRefId(friendship.getDirectRoomId());
            notification.setIsRead(0);
            notification.setCreatedAt(now);
            notificationMapper.insert(notification);
        } else {
            Notification notification = new Notification();
            notification.setUserId(entity.getRequesterId());
            notification.setType(NOTIFY_FRIEND_RESULT);
            notification.setTitle("好友申请未通过");
            notification.setContent((receiver == null ? "对方" : receiver.getUsername()) + " 暂未通过你的好友申请。");
            notification.setRefType(REF_USER);
            notification.setRefId(entity.getReceiverId());
            notification.setIsRead(0);
            notification.setCreatedAt(now);
            notificationMapper.insert(notification);
        }
    }

    private Long createDirectRoom(Long requesterId, Long receiverId, LocalDateTime now) {
        ChatRoom room = new ChatRoom();
        room.setName("好友私聊");
        room.setDescription("好友私聊房间");
        room.setOwnerId(requesterId);
        room.setRoomType(ROOM_TYPE_DIRECT);
        room.setStatus(ROOM_STATUS_NORMAL);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        chatRoomMapper.insert(room);

        ChatRoomMember requesterMember = new ChatRoomMember();
        requesterMember.setRoomId(room.getId());
        requesterMember.setUserId(requesterId);
        requesterMember.setRoleType(MEMBER_ROLE_NORMAL);
        requesterMember.setJoinedAt(now);
        chatRoomMemberMapper.insert(requesterMember);

        ChatRoomMember receiverMember = new ChatRoomMember();
        receiverMember.setRoomId(room.getId());
        receiverMember.setUserId(receiverId);
        receiverMember.setRoleType(MEMBER_ROLE_NORMAL);
        receiverMember.setJoinedAt(now);
        chatRoomMemberMapper.insert(receiverMember);

        return room.getId();
    }

    private String buildRequestContent(String requesterName, String message) {
        String prefix = (requesterName == null || requesterName.trim().isEmpty() ? "有同学" : requesterName.trim()) + " 想添加你为好友";
        if (message == null || message.trim().isEmpty()) {
            return prefix + "。";
        }
        return prefix + "：" + message.trim();
    }

    private Long normalizeUserOne(Long left, Long right) {
        return left < right ? left : right;
    }

    private Long normalizeUserTwo(Long left, Long right) {
        return left < right ? right : left;
    }
}

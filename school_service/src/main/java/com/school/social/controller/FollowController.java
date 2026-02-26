package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.auth.UserView;
import com.school.social.dto.interaction.FollowStatsResponse;
import com.school.social.entity.Follow;
import com.school.social.entity.Notification;
import com.school.social.entity.User;
import com.school.social.mapper.FollowMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.UserMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/follows")
public class FollowController {
    private static final int NOTIFY_FOLLOW = 3;
    private static final int REF_USER = 2;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @PostMapping("/{followeeId}")
    public ApiResponse<Void> follow(@PathVariable Long followeeId,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (followeeId == null) {
            return ApiResponse.fail("followeeId 不能为空");
        }
        if (userId.equals(followeeId)) {
            return ApiResponse.fail("不能关注自己");
        }
        User target = userMapper.selectById(followeeId);
        if (target == null) {
            return ApiResponse.fail("用户不存在");
        }
        Follow existing = followMapper.selectByFollowerAndFollowee(userId, followeeId);
        if (existing != null) {
            return ApiResponse.success(null);
        }
        Follow follow = new Follow();
        follow.setFollowerId(userId);
        follow.setFolloweeId(followeeId);
        follow.setCreatedAt(LocalDateTime.now());
        followMapper.insert(follow);
        createFollowNotification(userId, followeeId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{followeeId}")
    public ApiResponse<Void> unfollow(@PathVariable Long followeeId,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (followeeId == null) {
            return ApiResponse.fail("followeeId 不能为空");
        }
        Follow existing = followMapper.selectByFollowerAndFollowee(userId, followeeId);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        followMapper.deleteByFollowerAndFollowee(userId, followeeId);
        return ApiResponse.success(null);
    }

    @GetMapping("/following")
    public ApiResponse<PageResponse<UserView>> following(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<UserView> list = followMapper.selectFollowingPaged(userId, offset, safeSize);
        long total = followMapper.countFollowing(userId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/followers")
    public ApiResponse<PageResponse<UserView>> followers(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<UserView> list = followMapper.selectFollowersPaged(userId, offset, safeSize);
        long total = followMapper.countFollowers(userId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/stats")
    public ApiResponse<FollowStatsResponse> stats(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int following = followMapper.countFollowing(userId);
        int followers = followMapper.countFollowers(userId);
        return ApiResponse.success(new FollowStatsResponse(following, followers));
    }

    private void createFollowNotification(Long followerId, Long followeeId) {
        if (followeeId == null || followeeId.equals(followerId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(followeeId);
        notification.setType(NOTIFY_FOLLOW);
        notification.setTitle("新增关注");
        notification.setContent("有用户关注了你");
        notification.setRefType(REF_USER);
        notification.setRefId(followerId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}


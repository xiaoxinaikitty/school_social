package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.entity.Notification;
import com.school.social.mapper.NotificationMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Resource
    private NotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<PageResponse<Notification>> list(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Integer isRead,
                                                        HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Notification> list = notificationMapper.selectByUserPaged(userId, isRead, offset, safeSize);
        long total = notificationMapper.countByUser(userId, isRead);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int count = notificationMapper.countUnread(userId);
        return ApiResponse.success(count);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        notificationMapper.markRead(id, userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        notificationMapper.markAllRead(userId);
        return ApiResponse.success(null);
    }
}


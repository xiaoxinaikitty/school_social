package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.admin.PostReviewRequest;
import com.school.social.dto.admin.PostFlagRequest;
import com.school.social.entity.AuditLog;
import com.school.social.entity.Notification;
import com.school.social.entity.Post;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.AuditLogMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_DRAFT = 3;
    private static final int NOTIFY_AUDIT = 4;
    private static final int REF_POST = 0;

    @Resource
    private PostMapper postMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private AuditLogMapper auditLogMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<PageResponse<Post>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Integer status,
                                                HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        Integer queryStatus = status;
        if (queryStatus == null) {
            queryStatus = STATUS_PENDING;
        }
        List<Post> list = postMapper.selectPaged(queryStatus, offset, safeSize);
        long total = postMapper.countByStatus(queryStatus);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Post> review(@PathVariable Long id,
                                    @RequestBody PostReviewRequest request,
                                    HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("userId");
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null || request.getDecision() == null) {
            return ApiResponse.fail("decision 不能为空");
        }
        int decision = request.getDecision();
        if (decision != STATUS_APPROVED && decision != STATUS_REJECTED) {
            return ApiResponse.fail("decision 不合法");
        }
        Post post = postMapper.selectById(id);
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (post.getStatus() != null && post.getStatus() == STATUS_DRAFT) {
            return ApiResponse.fail("草稿内容不可审核");
        }
        Post update = new Post();
        update.setId(id);
        update.setStatus(decision);
        update.setUpdatedAt(LocalDateTime.now());
        if (decision == STATUS_APPROVED && post.getPublishedAt() == null) {
            update.setPublishedAt(LocalDateTime.now());
        }
        postMapper.updateById(update);

        AuditLog log = new AuditLog();
        log.setTargetType(REF_POST);
        log.setTargetId(id);
        log.setAuditorId(adminId);
        log.setDecision(decision);
        log.setNote(request.getNote());
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);

        createAuditNotification(post.getUserId(), id, decision, request.getNote());

        Post refreshed = postMapper.selectById(id);
        return ApiResponse.success(refreshed);
    }

    @PutMapping("/{id}/flags")
    public ApiResponse<Post> updateFlags(@PathVariable Long id,
                                         @RequestBody PostFlagRequest request,
                                         HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null) {
            return ApiResponse.fail("参数不能为空");
        }
        Post existing = postMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("内容不存在");
        }
        Post update = new Post();
        update.setId(id);
        update.setIsPinned(request.getPinned());
        update.setIsFeatured(request.getFeatured());
        update.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(update);
        return ApiResponse.success(postMapper.selectById(id));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return false;
        }
        Role role = roleMapper.selectByName("admin");
        if (role == null) {
            return false;
        }
        UserRole link = userRoleMapper.selectByPk(userId, role.getId());
        return link != null;
    }

    private void createAuditNotification(Long userId, Long postId, int decision, String note) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NOTIFY_AUDIT);
        notification.setTitle(decision == STATUS_APPROVED ? "内容审核通过" : "内容审核驳回");
        String content = decision == STATUS_APPROVED ? "你的内容已审核通过" : "你的内容未通过审核";
        if (note != null && !note.trim().isEmpty()) {
            content = content + "：" + note.trim();
        }
        notification.setContent(content);
        notification.setRefType(REF_POST);
        notification.setRefId(postId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}

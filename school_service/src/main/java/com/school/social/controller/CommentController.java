package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.interaction.CommentCreateRequest;
import com.school.social.entity.Comment;
import com.school.social.entity.Notification;
import com.school.social.entity.Post;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.CommentMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private static final int NOTIFY_COMMENT = 1;
    private static final int NOTIFY_REPLY = 2;
    private static final int REF_POST = 0;
    private static final int REF_COMMENT = 1;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @PostMapping
    public ApiResponse<Comment> create(@RequestBody CommentCreateRequest request,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getPostId() == null || request.getContent() == null
                || request.getContent().trim().isEmpty()) {
            return ApiResponse.fail("参数不能为空");
        }
        Post post = postMapper.selectById(request.getPostId());
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (post.getStatus() == null || post.getStatus() != 1) {
            if (!isOwnerOrAdmin(userId, post.getUserId())) {
                return ApiResponse.fail("内容未审核通过");
            }
        }
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null) {
                return ApiResponse.fail("回复的评论不存在");
            }
            if (!parent.getPostId().equals(request.getPostId())) {
                return ApiResponse.fail("回复的评论不属于该内容");
            }
        }
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setStatus(0);
        comment.setLikeCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        postMapper.increaseCommentCount(request.getPostId());
        createCommentNotification(userId, request, post);
        return ApiResponse.success(comment);
    }

    @GetMapping
    public ApiResponse<PageResponse<Comment>> list(@RequestParam Long postId,
                                                   @RequestParam(required = false) Long parentId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   HttpServletRequest httpRequest) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (post.getStatus() == null || post.getStatus() != 1) {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (!isOwnerOrAdmin(userId, post.getUserId())) {
                return ApiResponse.fail("内容未审核通过");
            }
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Comment> list = commentMapper.selectByPostIdPaged(postId, parentId, offset, safeSize);
        long total = commentMapper.countByPostId(postId, parentId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Comment existing = commentMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        if (!userId.equals(existing.getUserId())) {
            return ApiResponse.fail("无权限操作");
        }
        int removed = 1;
        if (existing.getParentId() == null) {
            int replyCount = commentMapper.countByParentId(id);
            if (replyCount > 0) {
                commentMapper.deleteByParentId(id);
                removed += replyCount;
            }
        }
        commentMapper.deleteById(id);
        postMapper.decreaseCommentCountBy(existing.getPostId(), removed);
        return ApiResponse.success(null);
    }

    private void createCommentNotification(Long actorId, CommentCreateRequest request, Post post) {
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(actorId)) {
                Notification notification = new Notification();
                notification.setUserId(parent.getUserId());
                notification.setType(NOTIFY_REPLY);
                notification.setTitle("收到回复");
                notification.setContent("有人回复了你的评论");
                notification.setRefType(REF_COMMENT);
                notification.setRefId(parent.getId());
                notification.setIsRead(0);
                notification.setCreatedAt(LocalDateTime.now());
                notificationMapper.insert(notification);
            }
            return;
        }
        if (post.getUserId() == null || post.getUserId().equals(actorId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(post.getUserId());
        notification.setType(NOTIFY_COMMENT);
        notification.setTitle("收到评论");
        notification.setContent("有人评论了你的内容");
        notification.setRefType(REF_POST);
        notification.setRefId(post.getId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private boolean isOwnerOrAdmin(Long userId, Long ownerId) {
        if (userId == null) {
            return false;
        }
        if (ownerId != null && ownerId.equals(userId)) {
            return true;
        }
        Role role = roleMapper.selectByName("admin");
        if (role == null) {
            return false;
        }
        UserRole link = userRoleMapper.selectByPk(userId, role.getId());
        return link != null;
    }
}


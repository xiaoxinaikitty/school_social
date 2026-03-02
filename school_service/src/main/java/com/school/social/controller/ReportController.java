package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.report.ReportCreateRequest;
import com.school.social.dto.admin.ReportHandleRequest;
import com.school.social.entity.Report;
import com.school.social.entity.Notification;
import com.school.social.entity.Post;
import com.school.social.entity.Comment;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import com.school.social.mapper.ReportMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.CommentMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final int TARGET_POST = 0;
    private static final int TARGET_COMMENT = 1;
    private static final int TARGET_USER = 2;
    private static final int STATUS_HANDLED = 1;
    private static final int DECISION_VALID = 1;
    private static final int DECISION_INVALID = 2;
    private static final int NOTIFY_SYSTEM = 4;
    private static final int REF_POST = 0;
    private static final int REF_COMMENT = 1;
    private static final int REF_USER = 2;

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @PostMapping
    public ApiResponse<Report> create(@RequestBody ReportCreateRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getTargetType() == null || request.getTargetId() == null
                || request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ApiResponse.fail("参数不能为空");
        }
        if (request.getTargetType() != TARGET_POST
                && request.getTargetType() != TARGET_COMMENT
                && request.getTargetType() != TARGET_USER) {
            return ApiResponse.fail("targetType 不合法");
        }
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setDetail(request.getDetail());
        report.setStatus(0);
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        return ApiResponse.success(report);
    }

    @GetMapping
    public ApiResponse<PageResponse<Report>> myReports(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status,
                                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Report> list = reportMapper.selectByReporterPaged(userId, status, offset, safeSize);
        long total = reportMapper.countByReporter(userId, status);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/admin")
    public ApiResponse<PageResponse<Report>> adminList(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status,
                                                       HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Report> list = reportMapper.selectAllPaged(status, offset, safeSize);
        long total = reportMapper.countAll(status);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @PutMapping("/admin/{id}/handle")
    public ApiResponse<Report> handle(@PathVariable Long id,
                                      @RequestBody ReportHandleRequest request,
                                      HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("userId");
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null || request.getDecision() == null) {
            return ApiResponse.fail("decision 不能为空");
        }
        int decision = request.getDecision();
        if (decision != DECISION_VALID && decision != DECISION_INVALID) {
            return ApiResponse.fail("decision 不合法");
        }
        Report existing = reportMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("举报不存在");
        }

        Long targetUserId = null;
        if (existing.getTargetType() != null && existing.getTargetType() == TARGET_POST) {
            Post post = postMapper.selectById(existing.getTargetId());
            if (post != null) {
                targetUserId = post.getUserId();
                if (decision == DECISION_VALID) {
                    Post updatePost = new Post();
                    updatePost.setId(post.getId());
                    updatePost.setStatus(2);
                    updatePost.setUpdatedAt(LocalDateTime.now());
                    postMapper.updateById(updatePost);
                }
            }
        } else if (existing.getTargetType() != null && existing.getTargetType() == TARGET_COMMENT) {
            Comment comment = commentMapper.selectById(existing.getTargetId());
            if (comment != null) {
                targetUserId = comment.getUserId();
            }
        } else if (existing.getTargetType() != null && existing.getTargetType() == TARGET_USER) {
            targetUserId = existing.getTargetId();
        }

        Report update = new Report();
        update.setId(id);
        update.setStatus(STATUS_HANDLED);
        update.setDecision(decision);
        update.setHandledBy(adminId);
        update.setHandledAt(LocalDateTime.now());
        update.setResult(request.getResult());
        reportMapper.updateById(update);

        if (decision == DECISION_VALID) {
            notifyReportResult(existing.getReporterId(), existing, decision, request.getResult(), true);
            if (targetUserId != null && !targetUserId.equals(existing.getReporterId())) {
                notifyReportResult(targetUserId, existing, decision, request.getResult(), false);
            }
        } else {
            notifyReportResult(existing.getReporterId(), existing, decision, request.getResult(), true);
        }

        return ApiResponse.success(reportMapper.selectById(id));
    }

    
    private void notifyReportResult(Long userId, Report report, int decision, String result, boolean isReporter) {
        if (userId == null || report == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NOTIFY_SYSTEM);
        String targetName = report.getTargetType() != null && report.getTargetType() == TARGET_POST
                ? "内容"
                : (report.getTargetType() != null && report.getTargetType() == TARGET_COMMENT ? "评论" : "用户");
        String title;
        String content;
        if (isReporter) {
            title = decision == DECISION_VALID ? "举报属实" : "举报不属实";
            content = "你提交的举报已处理：" + (decision == DECISION_VALID ? "属实" : "不属实") + "，对象" + targetName + " #" + report.getTargetId();
        } else {
            title = "举报处理结果";
            content = "你的" + targetName + " #" + report.getTargetId() + "被举报，经处理" + (decision == DECISION_VALID ? "属实" : "不属实");
        }
        if (result != null && !result.trim().isEmpty()) {
            content = content + "，处理说明：" + result.trim();
        }
        notification.setTitle(title);
        notification.setContent(content);
        int refType = REF_USER;
        if (report.getTargetType() != null) {
            if (report.getTargetType() == TARGET_POST) {
                refType = REF_POST;
            } else if (report.getTargetType() == TARGET_COMMENT) {
                refType = REF_COMMENT;
            }
        }
        notification.setRefType(refType);
        notification.setRefId(report.getTargetId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }    private boolean isAdmin(HttpServletRequest request) {
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
}








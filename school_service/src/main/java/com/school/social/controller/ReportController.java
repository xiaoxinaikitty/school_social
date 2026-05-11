package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.report.ReportCreateRequest;
import com.school.social.dto.report.ReportTargetOption;
import com.school.social.dto.report.ReportView;
import com.school.social.dto.admin.ReportHandleRequest;
import com.school.social.entity.Report;
import com.school.social.entity.Notification;
import com.school.social.entity.Post;
import com.school.social.entity.Comment;
import com.school.social.entity.Role;
import com.school.social.entity.User;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import com.school.social.mapper.ReportMapper;
import com.school.social.mapper.NotificationMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.CommentMapper;
import com.school.social.mapper.UserMapper;
import com.school.social.service.ContentDeletionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final int TARGET_POST = 0;
    private static final int TARGET_COMMENT = 1;
    private static final int TARGET_USER = 2;
    private static final int TARGET_ADMIN_FEEDBACK = 3;
    private static final int STATUS_HANDLED = 1;
    private static final int DECISION_VALID = 1;
    private static final int DECISION_INVALID = 2;
    private static final int NOTIFY_REPORT_RESULT = 7;
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

    @Resource
    private ContentDeletionService contentDeletionService;

    @Resource
    private UserMapper userMapper;

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
                && request.getTargetType() != TARGET_USER
                && request.getTargetType() != TARGET_ADMIN_FEEDBACK) {
            return ApiResponse.fail("targetType 不合法");
        }
        String validateMessage = validateReportTarget(userId, request.getTargetType(), request.getTargetId());
        if (validateMessage != null) {
            return ApiResponse.fail(validateMessage);
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
    public ApiResponse<PageResponse<ReportView>> myReports(@RequestParam(defaultValue = "1") int page,
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
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, buildReportViews(list)));
    }

    @GetMapping("/admin")
    public ApiResponse<PageResponse<ReportView>> adminList(@RequestParam(defaultValue = "1") int page,
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
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, buildReportViews(list)));
    }

    @GetMapping("/targets")
    public ApiResponse<List<ReportTargetOption>> searchTargets(@RequestParam Integer targetType,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "10") int limit,
                                                               HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (targetType == null
                || (targetType != TARGET_POST
                && targetType != TARGET_COMMENT
                && targetType != TARGET_USER
                && targetType != TARGET_ADMIN_FEEDBACK)) {
            return ApiResponse.fail("targetType 不支持搜索");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        String q = keyword == null ? null : keyword.trim();
        if (q == null || q.isEmpty()) {
            return ApiResponse.success(new ArrayList<>());
        }
        List<ReportTargetOption> result = new ArrayList<>();
        if (targetType == TARGET_POST) {
            List<Post> posts = postMapper.searchPaged(q, null, 0, safeLimit);
            for (Post post : posts) {
                ReportTargetOption option = new ReportTargetOption();
                option.setId(post.getId());
                option.setDisplayName((post.getTitle() == null || post.getTitle().trim().isEmpty()) ? "未命名内容" : post.getTitle());
                option.setSubtitle(buildPostOptionSubtitle(post));
                result.add(option);
            }
            return ApiResponse.success(result);
        }
        if (targetType == TARGET_COMMENT) {
            return ApiResponse.success(commentMapper.searchReportTargetOptions(q, 0, safeLimit));
        }
        Role adminRole = roleMapper.selectByName("admin");
        Long adminRoleId = adminRole == null ? null : adminRole.getId();
        List<com.school.social.dto.auth.UserView> candidates = userMapper.searchUserViews(
                q,
                targetType == TARGET_USER ? userId : 0L,
                0,
                safeLimit * 3
        );
        for (com.school.social.dto.auth.UserView item : candidates) {
            boolean isAdminUser = adminRoleId != null && userRoleMapper.selectByPk(item.getId(), adminRoleId) != null;
            if (targetType == TARGET_USER && (isAdminUser || item.getId().equals(userId))) {
                continue;
            }
            if (targetType == TARGET_ADMIN_FEEDBACK && !isAdminUser) {
                continue;
            }
            ReportTargetOption option = new ReportTargetOption();
            option.setId(item.getId());
            option.setDisplayName(item.getUsername());
            option.setSubtitle((item.getSchool() == null ? "未填学校" : item.getSchool()) + " / "
                    + (item.getCollege() == null ? "未填学院" : item.getCollege()));
            option.setAvatarUrl(item.getAvatarUrl());
            option.setSchool(item.getSchool());
            option.setCollege(item.getCollege());
            result.add(option);
            if (result.size() >= safeLimit) {
                break;
            }
        }
        return ApiResponse.success(result);
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
        if (existing.getStatus() != null && existing.getStatus() == STATUS_HANDLED) {
            return ApiResponse.success(existing);
        }

        Long targetUserId = null;
        if (existing.getTargetType() != null && existing.getTargetType() == TARGET_POST) {
            Post post = postMapper.selectById(existing.getTargetId());
            if (post != null) {
                targetUserId = post.getUserId();
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
            if (existing.getTargetType() != null && existing.getTargetType() == TARGET_POST) {
                contentDeletionService.deletePost(existing.getTargetId());
            } else if (existing.getTargetType() != null && existing.getTargetType() == TARGET_COMMENT) {
                contentDeletionService.deleteComment(existing.getTargetId());
            }
            notifyReportResult(existing.getReporterId(), existing, decision, request.getResult(), true);
            if (targetUserId != null
                    && existing.getTargetType() != null
                    && existing.getTargetType() != TARGET_ADMIN_FEEDBACK
                    && !targetUserId.equals(existing.getReporterId())) {
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
        notification.setType(NOTIFY_REPORT_RESULT);
        String targetName = getTargetTypeLabel(report.getTargetType());
        String title;
        String content;
        if (isReporter && report.getTargetType() != null && report.getTargetType() == TARGET_ADMIN_FEEDBACK) {
            title = decision == DECISION_VALID ? "反馈已采纳" : "反馈已处理";
            content = "你提交的管理员反馈已处理：" + (decision == DECISION_VALID ? "已采纳" : "已记录");
        } else if (isReporter) {
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
    }

    private List<ReportView> buildReportViews(List<Report> reports) {
        List<ReportView> result = new ArrayList<>();
        if (reports == null) {
            return result;
        }
        for (Report item : reports) {
            result.add(buildReportView(item));
        }
        return result;
    }

    private ReportView buildReportView(Report report) {
        ReportView view = new ReportView();
        view.setId(report.getId());
        view.setReporterId(report.getReporterId());
        view.setTargetType(report.getTargetType());
        view.setTargetId(report.getTargetId());
        view.setReason(report.getReason());
        view.setDetail(report.getDetail());
        view.setStatus(report.getStatus());
        view.setDecision(report.getDecision());
        view.setHandledBy(report.getHandledBy());
        view.setHandledAt(report.getHandledAt());
        view.setResult(report.getResult());
        view.setCreatedAt(report.getCreatedAt());

        User reporter = report.getReporterId() == null ? null : userMapper.selectById(report.getReporterId());
        view.setReporterName(reporter == null ? null : reporter.getUsername());
        view.setTargetName(resolveTargetName(report));
        return view;
    }

    private String resolveTargetName(Report report) {
        if (report == null || report.getTargetType() == null || report.getTargetId() == null) {
            return null;
        }
        if (report.getTargetType() == TARGET_POST) {
            Post post = postMapper.selectById(report.getTargetId());
            if (post == null) {
                return null;
            }
            if (post.getTitle() != null && !post.getTitle().trim().isEmpty()) {
                return post.getTitle();
            }
            if (post.getContent() != null && !post.getContent().trim().isEmpty()) {
                return clipText(post.getContent(), 24);
            }
            return "未命名内容";
        }
        if (report.getTargetType() == TARGET_COMMENT) {
            Comment comment = commentMapper.selectById(report.getTargetId());
            if (comment == null) {
                return null;
            }
            return clipText(comment.getContent(), 24);
        }
        User user = userMapper.selectById(report.getTargetId());
        return user == null ? null : user.getUsername();
    }

    private String validateReportTarget(Long reporterId, Integer targetType, Long targetId) {
        if (targetType == TARGET_POST) {
            return postMapper.selectById(targetId) == null ? "举报内容不存在" : null;
        }
        if (targetType == TARGET_COMMENT) {
            return commentMapper.selectById(targetId) == null ? "举报评论不存在" : null;
        }
        User user = userMapper.selectById(targetId);
        if (user == null) {
            return targetType == TARGET_ADMIN_FEEDBACK ? "反馈管理员不存在" : "举报用户不存在";
        }
        if (reporterId != null && reporterId.equals(targetId)) {
            return targetType == TARGET_ADMIN_FEEDBACK ? "不能给自己提交管理员反馈" : "不能举报自己";
        }
        boolean adminUser = isAdminUser(targetId);
        if (targetType == TARGET_USER && adminUser) {
            return "管理员反馈请使用“管理员反馈”类型";
        }
        if (targetType == TARGET_ADMIN_FEEDBACK && !adminUser) {
            return "请选择管理员账号";
        }
        return null;
    }

    private boolean isAdminUser(Long userId) {
        if (userId == null) {
            return false;
        }
        Role role = roleMapper.selectByName("admin");
        if (role == null) {
            return false;
        }
        return userRoleMapper.selectByPk(userId, role.getId()) != null;
    }

    private String getTargetTypeLabel(Integer targetType) {
        if (targetType != null && targetType == TARGET_POST) {
            return "内容";
        }
        if (targetType != null && targetType == TARGET_COMMENT) {
            return "评论";
        }
        if (targetType != null && targetType == TARGET_ADMIN_FEEDBACK) {
            return "管理员反馈";
        }
        return "用户";
    }

    private String buildPostOptionSubtitle(Post post) {
        if (post == null) {
            return null;
        }
        User author = post.getUserId() == null ? null : userMapper.selectById(post.getUserId());
        String authorName = author == null || author.getUsername() == null ? "未知用户" : author.getUsername();
        String preview = post.getContent() == null ? "" : clipText(post.getContent(), 30);
        return "作者：" + authorName + (preview.isEmpty() ? "" : " / " + preview);
    }

    private String clipText(String value, int max) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return text;
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
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
}








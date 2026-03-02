package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.report.ReportCreateRequest;
import com.school.social.dto.admin.ReportHandleRequest;
import com.school.social.entity.Report;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import com.school.social.mapper.ReportMapper;
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

    @Resource
    private ReportMapper reportMapper;

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
        Report existing = reportMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("举报不存在");
        }
        Report update = new Report();
        update.setId(id);
        update.setStatus(1);
        update.setHandledBy(adminId);
        update.setHandledAt(LocalDateTime.now());
        update.setResult(request.getResult());
        reportMapper.updateById(update);
        return ApiResponse.success(reportMapper.selectById(id));
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


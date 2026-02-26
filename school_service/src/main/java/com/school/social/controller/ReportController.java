package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.report.ReportCreateRequest;
import com.school.social.entity.Report;
import com.school.social.mapper.ReportMapper;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/reports")
public class ReportController {
    private static final int TARGET_POST = 0;
    private static final int TARGET_COMMENT = 1;
    private static final int TARGET_USER = 2;

    @Resource
    private ReportMapper reportMapper;

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
}


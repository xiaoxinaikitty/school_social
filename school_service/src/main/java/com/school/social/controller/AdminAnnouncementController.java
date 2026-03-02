package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.admin.AnnouncementRequest;
import com.school.social.entity.Announcement;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.AnnouncementMapper;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {
    @Resource
    private AnnouncementMapper announcementMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @GetMapping
    public ApiResponse<PageResponse<Announcement>> list(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Integer status,
                                                        HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Announcement> list = announcementMapper.selectPaged(status, offset, safeSize);
        long total = announcementMapper.countByStatus(status);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @PostMapping
    public ApiResponse<Announcement> create(@RequestBody AnnouncementRequest request,
                                            HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()
                || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ApiResponse.fail("标题和内容不能为空");
        }
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        announcement.setStartAt(request.getStartAt());
        announcement.setEndAt(request.getEndAt());
        announcement.setCreatedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return ApiResponse.success(announcement);
    }

    @PutMapping("/{id}")
    public ApiResponse<Announcement> update(@PathVariable Long id,
                                            @RequestBody AnnouncementRequest request,
                                            HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("公告不存在");
        }
        Announcement update = new Announcement();
        update.setId(id);
        if (request != null) {
            update.setTitle(request.getTitle());
            update.setContent(request.getContent());
            update.setStatus(request.getStatus());
            update.setStartAt(request.getStartAt());
            update.setEndAt(request.getEndAt());
        }
        announcementMapper.updateById(update);
        return ApiResponse.success(announcementMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        announcementMapper.deleteById(id);
        return ApiResponse.success(null);
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

package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.admin.UserStatusRequest;
import com.school.social.entity.Role;
import com.school.social.entity.User;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserMapper;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {
    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @GetMapping
    public ApiResponse<PageResponse<User>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<User> list = userMapper.selectPaged(keyword, status, offset, safeSize);
        long total = userMapper.countByCondition(keyword, status);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }
    @PutMapping("/{id}/status")
    public ApiResponse<User> updateStatus(@PathVariable Long id,
                                          @RequestBody UserStatusRequest request,
                                          HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null || request.getStatus() == null) {
            return ApiResponse.fail("status 不能为空");
        }
        User existing = userMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("用户不存在");
        }
        User update = new User();
        update.setId(id);
        update.setStatus(request.getStatus());
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);
        return ApiResponse.success(userMapper.selectById(id));
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


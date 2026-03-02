package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.config.RecommendConfigStore;
import com.school.social.dto.admin.RecommendConfig;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/recommend")
public class AdminRecommendController {
    @Resource
    private RecommendConfigStore recommendConfigStore;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @GetMapping("/config")
    public ApiResponse<RecommendConfig> getConfig(HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        return ApiResponse.success(recommendConfigStore.get());
    }

    @PutMapping("/config")
    public ApiResponse<RecommendConfig> update(@RequestBody RecommendConfig request,
                                               HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        RecommendConfig updated = recommendConfigStore.update(request);
        return ApiResponse.success(updated);
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

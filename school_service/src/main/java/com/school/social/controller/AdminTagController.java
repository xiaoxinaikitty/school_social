package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.admin.TagRequest;
import com.school.social.entity.Role;
import com.school.social.entity.Tag;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.TagMapper;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {
    @Resource
    private TagMapper tagMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @GetMapping
    public ApiResponse<List<Tag>> list(@RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Integer status,
                                       HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        List<Tag> list = tagMapper.selectAll();
        if (type != null) {
            list = list.stream().filter(tag -> type.equals(tag.getType())).collect(Collectors.toList());
        }
        if (status != null) {
            list = list.stream().filter(tag -> status.equals(tag.getStatus())).collect(Collectors.toList());
        }
        return ApiResponse.success(list);
    }

    @PostMapping
    public ApiResponse<Tag> create(@RequestBody TagRequest request, HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            return ApiResponse.fail("name 不能为空");
        }
        Tag tag = new Tag();
        tag.setName(request.getName().trim());
        tag.setType(request.getType() == null ? 0 : request.getType());
        tag.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        tag.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        return ApiResponse.success(tag);
    }

    @PutMapping("/{id}")
    public ApiResponse<Tag> update(@PathVariable Long id,
                                   @RequestBody TagRequest request,
                                   HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("标签不存在");
        }
        Tag update = new Tag();
        update.setId(id);
        if (request != null) {
            update.setName(request.getName());
            update.setType(request.getType());
            update.setStatus(request.getStatus());
        }
        tagMapper.updateById(update);
        return ApiResponse.success(tagMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        tagMapper.deleteById(id);
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

package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.user.UserProfileResponse;
import com.school.social.dto.user.UserProfileUpdateRequest;
import com.school.social.dto.user.UserTagUpdateRequest;
import com.school.social.dto.user.UserTagView;
import com.school.social.entity.User;
import com.school.social.entity.UserTag;
import com.school.social.entity.Post;
import com.school.social.entity.Comment;
import com.school.social.mapper.UserMapper;
import com.school.social.mapper.UserTagMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.FavoriteMapper;
import com.school.social.mapper.CommentMapper;
import com.school.social.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTagMapper userTagMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private CommentMapper commentMapper;

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable Long id, HttpServletRequest request) {
        if (!checkSelf(request, id)) {
            return ApiResponse.fail("无权限访问");
        }
        User user = userService.selectById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        return ApiResponse.success(toProfile(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserProfileResponse> updateProfile(@PathVariable Long id,
                                                          @Validated @RequestBody UserProfileUpdateRequest request,
                                                          HttpServletRequest httpRequest) {
        if (!checkSelf(httpRequest, id)) {
            return ApiResponse.fail("无权限访问");
        }
        User user = userService.selectById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User exists = userMapper.selectByEmail(request.getEmail());
            if (exists != null && !exists.getId().equals(id)) {
                return ApiResponse.fail("邮箱已被使用");
            }
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            User exists = userMapper.selectByPhone(request.getPhone());
            if (exists != null && !exists.getId().equals(id)) {
                return ApiResponse.fail("手机号已被使用");
            }
        }

        User update = new User();
        update.setId(id);
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setAvatarUrl(request.getAvatarUrl());
        update.setGender(request.getGender());
        update.setBirthday(request.getBirthday());
        update.setSchool(request.getSchool());
        update.setCollege(request.getCollege());
        update.setGrade(request.getGrade());
        update.setBio(request.getBio());
        update.setUpdatedAt(LocalDateTime.now());
        userService.updateById(update);

        User refreshed = userService.selectById(id);
        return ApiResponse.success(toProfile(refreshed));
    }

    @GetMapping("/{id}/tags")
    public ApiResponse<List<UserTagView>> getUserTags(@PathVariable Long id, HttpServletRequest request) {
        if (!checkSelf(request, id)) {
            return ApiResponse.fail("无权限访问");
        }
        List<UserTagView> tags = userTagMapper.selectByUserId(id);
        return ApiResponse.success(tags);
    }

    @PutMapping("/{id}/tags")
    public ApiResponse<List<UserTagView>> updateUserTags(@PathVariable Long id,
                                                         @RequestBody UserTagUpdateRequest request,
                                                         HttpServletRequest httpRequest) {
        if (!checkSelf(httpRequest, id)) {
            return ApiResponse.fail("无权限访问");
        }
        userTagMapper.deleteByUserId(id);
        if (request != null && request.getTagIds() != null) {
            for (Long tagId : request.getTagIds()) {
                if (tagId == null) {
                    continue;
                }
                UserTag userTag = new UserTag();
                userTag.setUserId(id);
                userTag.setTagId(tagId);
                userTag.setWeight(new BigDecimal("1.00"));
                userTagMapper.insert(userTag);
            }
        }
        return ApiResponse.success(userTagMapper.selectByUserId(id));
    }

    @GetMapping("/{id}/posts")
    public ApiResponse<PageResponse<Post>> getMyPosts(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      HttpServletRequest request) {
        if (!checkSelf(request, id)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectByUserIdPaged(id, offset, safeSize);
        long total = postMapper.countByUserId(id);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/{id}/favorites")
    public ApiResponse<PageResponse<Post>> getMyFavorites(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          HttpServletRequest request) {
        if (!checkSelf(request, id)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = favoriteMapper.selectFavoritePostsByUserIdPaged(id, offset, safeSize);
        long total = favoriteMapper.countFavoritePostsByUserId(id);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PageResponse<Comment>> getMyComments(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            HttpServletRequest request) {
        if (!checkSelf(request, id)) {
            return ApiResponse.fail("无权限访问");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Comment> list = commentMapper.selectByUserIdPaged(id, offset, safeSize);
        long total = commentMapper.countByUserId(id);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    private UserProfileResponse toProfile(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setGender(user.getGender());
        resp.setBirthday(user.getBirthday());
        resp.setSchool(user.getSchool());
        resp.setCollege(user.getCollege());
        resp.setGrade(user.getGrade());
        resp.setBio(user.getBio());
        resp.setStatus(user.getStatus());
        resp.setLastLoginAt(user.getLastLoginAt());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        return resp;
    }

    private boolean checkSelf(HttpServletRequest request, Long id) {
        Long tokenUserId = (Long) request.getAttribute("userId");
        return tokenUserId != null && tokenUserId.equals(id);
    }
}


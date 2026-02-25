package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.interaction.FavoriteRequest;
import com.school.social.entity.Favorite;
import com.school.social.entity.Post;
import com.school.social.mapper.FavoriteMapper;
import com.school.social.mapper.PostMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private PostMapper postMapper;

    @PostMapping
    public ApiResponse<Void> add(@RequestBody FavoriteRequest request,
                                 HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getPostId() == null) {
            return ApiResponse.fail("postId 不能为空");
        }
        Long postId = request.getPostId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        Favorite existing = favoriteMapper.selectByUserAndPost(userId, postId);
        if (existing != null) {
            return ApiResponse.success(null);
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        postMapper.increaseFavoriteCount(postId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> remove(@PathVariable Long postId,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (postId == null) {
            return ApiResponse.fail("postId 不能为空");
        }
        Favorite existing = favoriteMapper.selectByUserAndPost(userId, postId);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        favoriteMapper.deleteByUserAndPost(userId, postId);
        postMapper.decreaseFavoriteCount(postId);
        return ApiResponse.success(null);
    }
}


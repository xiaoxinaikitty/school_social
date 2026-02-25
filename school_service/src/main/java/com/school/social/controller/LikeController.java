package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.interaction.LikeRequest;
import com.school.social.entity.Comment;
import com.school.social.entity.Like;
import com.school.social.entity.Post;
import com.school.social.mapper.CommentMapper;
import com.school.social.mapper.LikeMapper;
import com.school.social.mapper.PostMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/likes")
public class LikeController {
    private static final int TARGET_POST = 0;
    private static final int TARGET_COMMENT = 1;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private CommentMapper commentMapper;

    @PostMapping
    public ApiResponse<Void> like(@RequestBody LikeRequest request,
                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getTargetType() == null || request.getTargetId() == null) {
            return ApiResponse.fail("参数不能为空");
        }
        Integer targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        if (targetType != TARGET_POST && targetType != TARGET_COMMENT) {
            return ApiResponse.fail("targetType 不合法");
        }
        Like existing = likeMapper.selectByUserAndTarget(userId, targetType, targetId);
        if (existing != null) {
            return ApiResponse.success(null);
        }
        if (targetType == TARGET_POST) {
            Post post = postMapper.selectById(targetId);
            if (post == null) {
                return ApiResponse.fail("内容不存在");
            }
        } else {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null) {
                return ApiResponse.fail("评论不存在");
            }
        }
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        like.setCreatedAt(LocalDateTime.now());
        likeMapper.insert(like);

        if (targetType == TARGET_POST) {
            postMapper.increaseLikeCount(targetId);
        } else {
            commentMapper.increaseLikeCount(targetId);
        }
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> unlike(@RequestParam Integer targetType,
                                    @RequestParam Long targetId,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (targetType == null || targetId == null) {
            return ApiResponse.fail("参数不能为空");
        }
        if (targetType != TARGET_POST && targetType != TARGET_COMMENT) {
            return ApiResponse.fail("targetType 不合法");
        }
        Like existing = likeMapper.selectByUserAndTarget(userId, targetType, targetId);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        likeMapper.deleteByUserAndTarget(userId, targetType, targetId);
        if (targetType == TARGET_POST) {
            postMapper.decreaseLikeCount(targetId);
        } else {
            commentMapper.decreaseLikeCount(targetId);
        }
        return ApiResponse.success(null);
    }
}


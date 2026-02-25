package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.interaction.ShareRequest;
import com.school.social.entity.BehaviorLog;
import com.school.social.entity.Post;
import com.school.social.mapper.BehaviorLogMapper;
import com.school.social.mapper.PostMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/shares")
public class ShareController {
    private static final int TARGET_POST = 0;
    private static final int ACTION_SHARE = 4;

    @Resource
    private PostMapper postMapper;

    @Resource
    private BehaviorLogMapper behaviorLogMapper;

    @PostMapping
    public ApiResponse<Void> share(@RequestBody ShareRequest request,
                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getPostId() == null) {
            return ApiResponse.fail("postId 不能为空");
        }
        Post post = postMapper.selectById(request.getPostId());
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        BehaviorLog log = new BehaviorLog();
        log.setUserId(userId);
        log.setActionType(ACTION_SHARE);
        log.setTargetType(TARGET_POST);
        log.setTargetId(request.getPostId());
        log.setCreatedAt(LocalDateTime.now());
        behaviorLogMapper.insert(log);
        return ApiResponse.success(null);
    }
}

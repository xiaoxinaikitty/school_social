package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.recommend.RecommendationFeedbackRequest;
import com.school.social.service.RecommendationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/recommendation-logs")
public class RecommendationLogController {
    @Resource
    private RecommendationService recommendationService;

    @PostMapping("/click")
    public ApiResponse<Void> recordClick(@RequestBody RecommendationFeedbackRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getPostId() == null) {
            return ApiResponse.fail("postId 不能为空");
        }
        recommendationService.recordClick(userId, request.getPostId(), request.getScene());
        return ApiResponse.success(null);
    }
}

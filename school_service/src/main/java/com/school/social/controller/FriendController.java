package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.friend.FriendCandidateView;
import com.school.social.dto.friend.FriendRequestCreateRequest;
import com.school.social.dto.friend.FriendRequestRespondRequest;
import com.school.social.service.FriendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    @Resource
    private FriendService friendService;

    @GetMapping("/search")
    public ApiResponse<PageResponse<FriendCandidateView>> search(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "8") int size,
                                                                 HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.success(friendService.searchCandidates(userId, keyword, page, size));
    }

    @PostMapping("/requests")
    public ApiResponse<Void> createRequest(@RequestBody FriendRequestCreateRequest request,
                                           HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            friendService.sendRequest(userId, request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/respond")
    public ApiResponse<Void> respondRequest(@PathVariable Long requestId,
                                            @RequestBody FriendRequestRespondRequest request,
                                            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            friendService.respondRequest(userId, requestId, request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

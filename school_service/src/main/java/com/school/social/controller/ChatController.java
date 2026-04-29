package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.chat.ChatMessageView;
import com.school.social.dto.chat.ChatRoomCreateRequest;
import com.school.social.dto.chat.ChatRoomView;
import com.school.social.service.ChatService;
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
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Resource
    private ChatService chatService;

    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomView>> listRooms(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.success(chatService.listRooms(userId));
    }

    @PostMapping("/rooms")
    public ApiResponse<ChatRoomView> createRoom(@RequestBody ChatRoomCreateRequest request,
                                                HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            return ApiResponse.success(chatService.createRoom(userId, request));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/rooms/{roomId}/join")
    public ApiResponse<Void> joinRoom(@PathVariable Long roomId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            chatService.joinRoom(userId, roomId);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/rooms/{roomId}/quit")
    public ApiResponse<Void> quitRoom(@PathVariable Long roomId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            chatService.quitRoom(userId, roomId);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<PageResponse<ChatMessageView>> listMessages(@PathVariable Long roomId,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "30") int size,
                                                                   HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            return ApiResponse.success(chatService.listMessages(userId, roomId, page, size));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PutMapping("/rooms/{roomId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long roomId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        try {
            chatService.markRoomRead(userId, roomId);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

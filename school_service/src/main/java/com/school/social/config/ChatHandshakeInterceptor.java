package com.school.social.config;

import com.school.social.common.JwtUtil;
import com.school.social.entity.User;
import com.school.social.mapper.UserMapper;
import com.school.social.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Map;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    private ChatService chatService;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String token = servletRequest.getParameter("token");
        String roomIdText = servletRequest.getParameter("roomId");
        Long userId = JwtUtil.getUserId(token);
        Long roomId = parseLong(roomIdText);
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null || !isTokenValid(token, user)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (roomId == null || !chatService.isMember(userId, roomId)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("roomId", roomId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isTokenValid(String token, User user) {
        if (user.getPasswordChangedAt() == null) {
            return true;
        }
        Long tokenPasswordChangedAt = JwtUtil.getPasswordChangedAtMillis(token);
        if (tokenPasswordChangedAt == null) {
            return false;
        }
        long currentPasswordChangedAt = user.getPasswordChangedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return currentPasswordChangedAt == tokenPasswordChangedAt;
    }
}

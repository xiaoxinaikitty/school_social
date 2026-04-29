package com.school.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.social.common.ApiResponse;
import com.school.social.dto.chat.ChatMessageView;
import com.school.social.dto.chat.ChatSocketMessageRequest;
import com.school.social.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    @Resource
    private ChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (roomId == null) {
            return;
        }
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            sendError(session, "连接已失效，请刷新页面重试。");
            return;
        }
        ChatSocketMessageRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), ChatSocketMessageRequest.class);
        } catch (Exception ex) {
            sendError(session, "消息格式不正确");
            return;
        }
        try {
            ChatMessageView saved = chatService.saveMessage(userId, roomId, request == null ? null : request.getContent());
            broadcast(roomId, objectMapper.writeValueAsString(ApiResponse.success(saved)));
        } catch (IllegalArgumentException ex) {
            sendError(session, ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (roomId == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId);
        }
    }

    private void broadcast(Long roomId, String payload) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ApiResponse.fail(message))));
    }
}

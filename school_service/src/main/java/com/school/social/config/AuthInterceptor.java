package com.school.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.social.common.ApiResponse;
import com.school.social.common.JwtUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthInterceptor implements HandlerInterceptor {
    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_TOKEN = "token";
    private static final String PREFIX = "Bearer ";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response);
            return false;
        }
        Long userId = JwtUtil.getUserId(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }
        request.setAttribute("userId", userId);
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader(HEADER_AUTH);
        if (auth != null && auth.startsWith(PREFIX)) {
            return auth.substring(PREFIX.length()).trim();
        }
        String token = request.getHeader(HEADER_TOKEN);
        if (token != null && !token.isEmpty()) {
            return token.trim();
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isEmpty()) {
            return queryToken.trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("未登录或登录已过期")));
    }
}

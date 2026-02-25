package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.JwtUtil;
import com.school.social.dto.auth.LoginRequest;
import com.school.social.dto.auth.LoginResponse;
import com.school.social.dto.auth.RegisterRequest;
import com.school.social.dto.auth.UserView;
import com.school.social.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserView> register(@Validated @RequestBody RegisterRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            String ip = resolveIp(httpRequest);
            return ApiResponse.success(authService.register(request, ip));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail("注册失败");
        }
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            String ip = resolveIp(httpRequest);
            UserView user = authService.login(request, ip);
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            return ApiResponse.success(new LoginResponse(token, user));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail("登录失败");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

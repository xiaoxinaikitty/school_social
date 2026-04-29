package com.school.social.service;

import com.school.social.dto.auth.LoginRequest;
import com.school.social.dto.auth.RegisterRequest;
import com.school.social.dto.auth.UserView;

public interface AuthService {
    UserView register(RegisterRequest request, String registerIp);
    UserView login(LoginRequest request, String loginIp);
    void sendForgotPasswordCode(String email, String requestIp);
    void verifyForgotPasswordCode(String email, String code);
    void resetPassword(String email, String code, String newPassword);
}

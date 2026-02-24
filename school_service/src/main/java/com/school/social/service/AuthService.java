package com.school.social.service;

import com.school.social.dto.auth.LoginRequest;
import com.school.social.dto.auth.RegisterRequest;
import com.school.social.dto.auth.UserView;

public interface AuthService {
    UserView register(RegisterRequest request, String registerIp);
    UserView login(LoginRequest request, String loginIp);
}

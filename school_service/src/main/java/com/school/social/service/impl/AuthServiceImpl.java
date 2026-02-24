package com.school.social.service.impl;

import com.school.social.common.PasswordUtil;
import com.school.social.dto.auth.LoginRequest;
import com.school.social.dto.auth.RegisterRequest;
import com.school.social.dto.auth.UserView;
import com.school.social.entity.User;
import com.school.social.mapper.UserMapper;
import com.school.social.service.AuthService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserView register(RegisterRequest request, String registerIp) {
        String username = request.getUsername().trim();
        if (userMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userMapper.selectByEmail(request.getEmail()) != null) {
                throw new IllegalArgumentException("邮箱已被使用");
            }
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (userMapper.selectByPhone(request.getPhone()) != null) {
                throw new IllegalArgumentException("手机号已被使用");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSchool(request.getSchool());
        user.setCollege(request.getCollege());
        user.setGrade(request.getGrade());
        user.setBio(request.getBio());
        user.setGender(request.getGender());
        user.setStatus(0);
        user.setRegisterIp(registerIp);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toUserView(user);
    }

    @Override
    public UserView login(LoginRequest request, String loginIp) {
        String account = request.getAccount().trim();
        User user = resolveUser(account, request.getLoginType());
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new IllegalArgumentException("账号已被限制登录");
        }
        if (!PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLastLoginAt(user.getId(), now);
        user.setLastLoginAt(now);
        return toUserView(user);
    }

    private User resolveUser(String account, String loginType) {
        String type = loginType == null ? "" : loginType.trim().toLowerCase();
        if ("email".equals(type)) {
            return userMapper.selectByEmail(account);
        }
        if ("phone".equals(type)) {
            return userMapper.selectByPhone(account);
        }
        if ("username".equals(type)) {
            return userMapper.selectByUsername(account);
        }
        if (account.contains("@")) {
            return userMapper.selectByEmail(account);
        }
        if (account.matches("^[0-9]{8,20}$")) {
            return userMapper.selectByPhone(account);
        }
        return userMapper.selectByUsername(account);
    }

    private UserView toUserView(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setAvatarUrl(user.getAvatarUrl());
        view.setGender(user.getGender());
        view.setSchool(user.getSchool());
        view.setCollege(user.getCollege());
        view.setGrade(user.getGrade());
        view.setBio(user.getBio());
        view.setStatus(user.getStatus());
        view.setLastLoginAt(user.getLastLoginAt());
        view.setCreatedAt(user.getCreatedAt());
        return view;
    }
}

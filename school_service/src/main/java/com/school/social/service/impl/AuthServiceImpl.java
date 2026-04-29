package com.school.social.service.impl;

import com.school.social.common.PasswordUtil;
import com.school.social.dto.auth.LoginRequest;
import com.school.social.dto.auth.RegisterRequest;
import com.school.social.dto.auth.UserView;
import com.school.social.entity.PasswordResetCode;
import com.school.social.entity.User;
import com.school.social.mapper.PasswordResetCodeMapper;
import com.school.social.mapper.UserMapper;
import com.school.social.service.AuthService;
import com.school.social.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String RESET_SCENE = "forgot_password";
    private static final int RESET_STATUS_ACTIVE = 0;
    private static final int RESET_STATUS_EXPIRED = 2;
    private static final int RESET_STATUS_INVALIDATED = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordResetCodeMapper passwordResetCodeMapper;

    @Resource
    private EmailService emailService;

    @Value("${app.auth.password-reset.code-expire-minutes:10}")
    private int codeExpireMinutes;

    @Value("${app.auth.password-reset.send-interval-seconds:60}")
    private int sendIntervalSeconds;

    @Value("${app.auth.password-reset.max-send-per-hour:10}")
    private int maxSendPerHour;

    @Value("${app.auth.password-reset.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    @Override
    public UserView register(RegisterRequest request, String registerIp) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        String phone = normalizeOptionalText(request.getPhone());
        if (userMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.selectByEmail(email) != null) {
            throw new IllegalArgumentException("邮箱已被使用");
        }
        if (phone != null) {
            if (userMapper.selectByPhone(phone) != null) {
                throw new IllegalArgumentException("手机号已被使用");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setSchool(normalizeOptionalText(request.getSchool()));
        user.setCollege(normalizeOptionalText(request.getCollege()));
        user.setGrade(normalizeOptionalText(request.getGrade()));
        user.setBio(normalizeOptionalText(request.getBio()));
        user.setGender(request.getGender());
        user.setStatus(0);
        user.setRegisterIp(registerIp);
        user.setPasswordChangedAt(now);
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
        if (user.getPasswordChangedAt() == null) {
            User update = new User();
            update.setId(user.getId());
            update.setPasswordChangedAt(now);
            update.setUpdatedAt(now);
            userMapper.updateById(update);
            user.setPasswordChangedAt(now);
        }
        userMapper.updateLastLoginAt(user.getId(), now);
        user.setLastLoginAt(now);
        return toUserView(user);
    }

    @Override
    public void sendForgotPasswordCode(String email, String requestIp) {
        String normalizedEmail = normalizeEmail(email);
        if (!emailService.isAvailable()) {
            throw new IllegalStateException("邮件服务未配置，暂时无法找回密码");
        }
        assertSendRateLimit(normalizedEmail, requestIp);

        User user = userMapper.selectByEmail(normalizedEmail);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        passwordResetCodeMapper.invalidateActiveByEmailAndScene(
                normalizedEmail,
                RESET_SCENE,
                RESET_STATUS_INVALIDATED,
                now
        );

        PasswordResetCode entity = new PasswordResetCode();
        entity.setUserId(user.getId());
        entity.setEmail(normalizedEmail);
        entity.setScene(RESET_SCENE);
        entity.setExpireAt(now.plusMinutes(codeExpireMinutes));
        entity.setStatus(RESET_STATUS_ACTIVE);
        entity.setAttemptCount(0);
        entity.setRequestIp(normalizeOptionalText(requestIp));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        String code = generateVerifyCode();
        entity.setCodeHash(PasswordUtil.hash(code));
        passwordResetCodeMapper.insert(entity);

        try {
            emailService.sendPasswordResetCode(normalizedEmail, user.getUsername(), code, codeExpireMinutes);
        } catch (RuntimeException ex) {
            passwordResetCodeMapper.invalidateActiveByEmailAndScene(
                    normalizedEmail,
                    RESET_SCENE,
                    RESET_STATUS_INVALIDATED,
                    LocalDateTime.now()
            );
            throw ex;
        }
    }

    @Override
    public void verifyForgotPasswordCode(String email, String code) {
        validateResetCode(normalizeEmail(email), code);
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        String password = newPassword == null ? "" : newPassword.trim();
        if (password.length() < 6 || password.length() > 32) {
            throw new IllegalArgumentException("新密码长度需为 6-32 位");
        }

        PasswordResetCode record = validateResetCode(normalizedEmail, code);
        User user = userMapper.selectByEmail(normalizedEmail);
        if (user == null) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        User update = new User();
        update.setId(user.getId());
        update.setPasswordHash(PasswordUtil.hash(password));
        update.setPasswordChangedAt(now);
        update.setUpdatedAt(now);
        userMapper.updateById(update);
        passwordResetCodeMapper.markUsed(record.getId(), now, now);
    }

    private User resolveUser(String account, String loginType) {
        String type = loginType == null ? "" : loginType.trim().toLowerCase();
        if ("email".equals(type)) {
            return userMapper.selectByEmail(normalizeEmail(account));
        }
        if ("phone".equals(type)) {
            return userMapper.selectByPhone(account);
        }
        if ("username".equals(type)) {
            return userMapper.selectByUsername(account);
        }
        if (account.contains("@")) {
            return userMapper.selectByEmail(normalizeEmail(account));
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

    private void assertSendRateLimit(String email, String requestIp) {
        PasswordResetCode latest = passwordResetCodeMapper.selectLatestActiveByEmailAndScene(email, RESET_SCENE);
        LocalDateTime now = LocalDateTime.now();
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(sendIntervalSeconds).isAfter(now)) {
            throw new IllegalArgumentException("验证码发送过于频繁，请稍后再试");
        }

        LocalDateTime hourAgo = now.minusHours(1);
        if (passwordResetCodeMapper.countByEmailSince(email, RESET_SCENE, hourAgo) >= maxSendPerHour) {
            throw new IllegalArgumentException("该邮箱发送次数过多，请 1 小时后再试");
        }
        String normalizedIp = normalizeOptionalText(requestIp);
        if (normalizedIp != null
                && passwordResetCodeMapper.countByIpSince(normalizedIp, RESET_SCENE, hourAgo) >= maxSendPerHour * 2) {
            throw new IllegalArgumentException("当前网络请求过于频繁，请稍后再试");
        }
    }

    private PasswordResetCode validateResetCode(String email, String code) {
        String verifyCode = code == null ? "" : code.trim();
        if (!verifyCode.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        PasswordResetCode record = passwordResetCodeMapper.selectLatestActiveByEmailAndScene(email, RESET_SCENE);
        if (record == null) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        if (record.getExpireAt() == null || record.getExpireAt().isBefore(now)) {
            passwordResetCodeMapper.invalidateActiveByEmailAndScene(email, RESET_SCENE, RESET_STATUS_EXPIRED, now);
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        int attempts = record.getAttemptCount() == null ? 0 : record.getAttemptCount();
        if (attempts >= maxVerifyAttempts) {
            passwordResetCodeMapper.invalidateActiveByEmailAndScene(email, RESET_SCENE, RESET_STATUS_INVALIDATED, now);
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        if (!PasswordUtil.matches(verifyCode, record.getCodeHash())) {
            passwordResetCodeMapper.incrementAttemptCount(record.getId(), now);
            if (attempts + 1 >= maxVerifyAttempts) {
                passwordResetCodeMapper.invalidateActiveByEmailAndScene(email, RESET_SCENE, RESET_STATUS_INVALIDATED, now);
            }
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        return record;
    }

    private String normalizeEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        return value;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateVerifyCode() {
        int value = RANDOM.nextInt(1000000);
        return String.format("%06d", value);
    }
}

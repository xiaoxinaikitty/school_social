package com.school.social.service;

public interface EmailService {
    boolean isAvailable();

    void sendPasswordResetCode(String email, String username, String code, int expireMinutes);
}

package com.school.social.dto.auth;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class ForgotPasswordSendCodeRequest {
    @NotBlank
    @Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

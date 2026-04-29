package com.school.social.service.impl;

import com.school.social.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${app.mail.host:}")
    private String host;

    @Value("${app.mail.port:587}")
    private int port;

    @Value("${app.mail.username:}")
    private String username;

    @Value("${app.mail.password:}")
    private String password;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.mail.use-ssl:true}")
    private boolean useSsl;

    @Override
    public boolean isAvailable() {
        return hasText(host) && hasText(fromAddress);
    }

    @Override
    public void sendPasswordResetCode(String email, String username, String code, int expireMinutes) {
        if (!isAvailable()) {
            throw new IllegalStateException("邮件服务未配置，暂时无法找回密码");
        }
        try {
            validateConfiguration();
            executeSendMail(email, username, code, expireMinutes);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalStateException("邮件发送失败，请稍后重试");
            }
            throw new IllegalStateException(message.trim());
        }
    }

    private String buildContent(String username, String code, int expireMinutes) {
        String displayName = (username == null || username.trim().isEmpty()) ? "同学" : username.trim();
        return "你好，" + displayName + "：\n\n"
                + "你正在申请重置校园社交平台账号密码。\n"
                + "本次验证码为：" + code + "\n"
                + "验证码 " + expireMinutes + " 分钟内有效，仅可使用一次。\n"
                + "如果这不是你本人操作，请忽略本邮件并尽快检查账号安全。";
    }

    private void executeSendMail(String email, String targetUsername, String code, int expireMinutes) throws Exception {
        String subject = "校园社交平台密码重置验证码";
        String body = buildContent(targetUsername, code, expireMinutes);

        List<String> command = new ArrayList<>();
        command.add("powershell");
        command.add("-NoProfile");
        command.add("-NonInteractive");
        command.add("-Command");
        command.add(buildPowerShellCommand(email, subject, body));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        String output = readOutput(process.getInputStream());
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("邮件发送超时");
        }
        if (process.exitValue() != 0) {
            String message = output == null ? "" : output.trim();
            if (message.isEmpty()) {
                throw new IllegalStateException("邮件发送失败，请检查 SMTP 配置");
            }
            throw new IllegalStateException(message);
        }
    }

    private void validateConfiguration() {
        if (useSsl && port == 465) {
            throw new IllegalStateException("当前邮件发送方式不支持 SMTP 465 隐式 SSL，请将 MAIL_PORT 改为 587，并保留 MAIL_USE_SSL=true");
        }
    }

    private String buildPowerShellCommand(String email, String subject, String body) {
        StringBuilder builder = new StringBuilder();
        if (hasText(username)) {
            builder.append("$sec=ConvertTo-SecureString '")
                    .append(escapePowerShellString(password))
                    .append("' -AsPlainText -Force;");
            builder.append("$cred=New-Object System.Management.Automation.PSCredential('")
                    .append(escapePowerShellString(username))
                    .append("',$sec);");
        }
        builder.append("Send-MailMessage");
        builder.append(" -To '").append(escapePowerShellString(email)).append("'");
        builder.append(" -From '").append(escapePowerShellString(fromAddress)).append("'");
        builder.append(" -Subject '").append(escapePowerShellString(subject)).append("'");
        builder.append(" -Body '").append(escapePowerShellString(body)).append("'");
        builder.append(" -SmtpServer '").append(escapePowerShellString(host)).append("'");
        builder.append(" -Port ").append(port);
        builder.append(" -Encoding UTF8");
        if (useSsl) {
            builder.append(" -UseSsl");
        }
        if (hasText(username)) {
            builder.append(" -Credential $cred");
        }
        return builder.toString();
    }

    private String readOutput(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String escapePowerShellString(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}

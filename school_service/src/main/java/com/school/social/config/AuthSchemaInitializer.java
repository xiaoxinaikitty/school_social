package com.school.social.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class AuthSchemaInitializer {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        ensureUsersPasswordChangedAtColumn();
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS password_reset_code ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id BIGINT NULL,"
                + "email VARCHAR(100) NOT NULL,"
                + "scene VARCHAR(50) NOT NULL,"
                + "code_hash VARCHAR(255) NOT NULL,"
                + "expire_at DATETIME NOT NULL,"
                + "used_at DATETIME NULL,"
                + "status TINYINT NOT NULL DEFAULT 0,"
                + "attempt_count INT NOT NULL DEFAULT 0,"
                + "request_ip VARCHAR(45) NULL,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL,"
                + "KEY idx_password_reset_email_scene (email, scene, status),"
                + "KEY idx_password_reset_ip_scene (request_ip, scene)"
                + ")");
    }

    private void ensureUsersPasswordChangedAtColumn() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'users' "
                        + "AND COLUMN_NAME = 'password_changed_at'",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL AFTER last_login_at");
        }
    }
}

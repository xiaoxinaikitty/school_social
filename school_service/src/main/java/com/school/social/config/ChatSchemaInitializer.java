package com.school.social.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class ChatSchemaInitializer {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS chat_room ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "name VARCHAR(100) NOT NULL,"
                + "avatar_url VARCHAR(255),"
                + "description VARCHAR(255),"
                + "owner_id BIGINT NOT NULL,"
                + "room_type TINYINT NOT NULL DEFAULT 0,"
                + "status TINYINT NOT NULL DEFAULT 0,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL"
                + ")");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS chat_room_member ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "room_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "role_type TINYINT NOT NULL DEFAULT 0,"
                + "mute_until DATETIME NULL,"
                + "last_read_message_id BIGINT NULL,"
                + "joined_at DATETIME NOT NULL,"
                + "UNIQUE KEY uk_chat_room_user (room_id, user_id),"
                + "KEY idx_chat_room_member_room (room_id),"
                + "KEY idx_chat_room_member_user (user_id)"
                + ")");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS chat_message ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "room_id BIGINT NOT NULL,"
                + "sender_id BIGINT NOT NULL,"
                + "content TEXT NOT NULL,"
                + "message_type TINYINT NOT NULL DEFAULT 0,"
                + "status TINYINT NOT NULL DEFAULT 0,"
                + "created_at DATETIME NOT NULL,"
                + "KEY idx_chat_message_room (room_id),"
                + "KEY idx_chat_message_sender (sender_id)"
                + ")");
    }
}

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
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS friend_request ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "requester_id BIGINT NOT NULL,"
                + "receiver_id BIGINT NOT NULL,"
                + "request_message VARCHAR(200) NULL,"
                + "status TINYINT NOT NULL DEFAULT 0,"
                + "processed_at DATETIME NULL,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL,"
                + "KEY idx_friend_request_receiver (receiver_id, status),"
                + "KEY idx_friend_request_requester (requester_id, status)"
                + ")");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS friendship ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_one_id BIGINT NOT NULL,"
                + "user_two_id BIGINT NOT NULL,"
                + "direct_room_id BIGINT NULL,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL,"
                + "UNIQUE KEY uk_friend_users (user_one_id, user_two_id),"
                + "KEY idx_friend_user_one (user_one_id),"
                + "KEY idx_friend_user_two (user_two_id)"
                + ")");
    }
}

package com.school.social.config;

import com.school.social.common.PasswordUtil;
import com.school.social.entity.Role;
import com.school.social.entity.User;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.UserMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class AdminSeeder implements CommandLineRunner {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ADMIN_ROLE = "admin";

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Override
    public void run(String... args) {
        Role role = roleMapper.selectByName(ADMIN_ROLE);
        if (role == null) {
            Role newRole = new Role();
            newRole.setName(ADMIN_ROLE);
            newRole.setDescription("系统管理员");
            newRole.setCreatedAt(LocalDateTime.now());
            roleMapper.insert(newRole);
            role = newRole;
        }

        User admin = userMapper.selectByUsername(ADMIN_USERNAME);
        if (admin == null) {
            User user = new User();
            user.setUsername(ADMIN_USERNAME);
            user.setPasswordHash(PasswordUtil.hash(ADMIN_PASSWORD));
            user.setStatus(0);
            user.setRegisterIp("system");
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
            admin = user;
        }

        if (admin != null && role != null) {
            UserRole existing = userRoleMapper.selectByPk(admin.getId(), role.getId());
            if (existing == null) {
                UserRole link = new UserRole();
                link.setUserId(admin.getId());
                link.setRoleId(role.getId());
                userRoleMapper.insert(link);
            }
        }
    }
}

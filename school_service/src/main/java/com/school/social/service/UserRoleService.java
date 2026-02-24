package com.school.social.service;

import java.util.List;
import com.school.social.entity.UserRole;

public interface UserRoleService {
    int insert(UserRole entity);
    int deleteByPk(Long userId, Long roleId);
    UserRole selectByPk(Long userId, Long roleId);
    List<UserRole> selectAll();
}

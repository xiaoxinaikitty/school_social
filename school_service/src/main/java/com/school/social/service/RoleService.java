package com.school.social.service;

import java.util.List;
import com.school.social.entity.Role;

public interface RoleService {
    int insert(Role entity);
    int updateById(Role entity);
    int deleteById(Long id);
    Role selectById(Long id);
    List<Role> selectAll();
}

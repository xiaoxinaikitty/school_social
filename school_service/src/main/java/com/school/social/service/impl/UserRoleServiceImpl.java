package com.school.social.service.impl;

import com.school.social.entity.UserRole;
import com.school.social.mapper.UserRoleMapper;
import com.school.social.service.UserRoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Resource
    private UserRoleMapper mapper;

    @Override
    public int insert(UserRole entity) {
        return mapper.insert(entity);
    }

    @Override
    public int deleteByPk(Long userId, Long roleId) {
        return mapper.deleteByPk(userId, roleId);
    }

    @Override
    public UserRole selectByPk(Long userId, Long roleId) {
        return mapper.selectByPk(userId, roleId);
    }

    @Override
    public List<UserRole> selectAll() {
        return mapper.selectAll();
    }
}

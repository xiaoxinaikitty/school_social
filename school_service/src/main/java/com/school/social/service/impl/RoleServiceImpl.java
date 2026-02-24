package com.school.social.service.impl;

import com.school.social.entity.Role;
import com.school.social.mapper.RoleMapper;
import com.school.social.service.RoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper mapper;

    @Override
    public int insert(Role entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Role entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Role selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Role> selectAll() {
        return mapper.selectAll();
    }
}

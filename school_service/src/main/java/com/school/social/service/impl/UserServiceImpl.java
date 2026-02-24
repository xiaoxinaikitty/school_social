package com.school.social.service.impl;

import com.school.social.entity.User;
import com.school.social.mapper.UserMapper;
import com.school.social.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper mapper;

    @Override
    public int insert(User entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(User entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public User selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<User> selectAll() {
        return mapper.selectAll();
    }
}

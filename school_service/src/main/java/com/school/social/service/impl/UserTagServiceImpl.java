package com.school.social.service.impl;

import com.school.social.entity.UserTag;
import com.school.social.mapper.UserTagMapper;
import com.school.social.service.UserTagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserTagServiceImpl implements UserTagService {

    @Resource
    private UserTagMapper mapper;

    @Override
    public int insert(UserTag entity) {
        return mapper.insert(entity);
    }

    @Override
    public int deleteByPk(Long userId, Long tagId) {
        return mapper.deleteByPk(userId, tagId);
    }

    @Override
    public UserTag selectByPk(Long userId, Long tagId) {
        return mapper.selectByPk(userId, tagId);
    }

    @Override
    public List<UserTag> selectAll() {
        return mapper.selectAll();
    }
}

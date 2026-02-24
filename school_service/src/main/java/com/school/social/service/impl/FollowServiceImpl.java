package com.school.social.service.impl;

import com.school.social.entity.Follow;
import com.school.social.mapper.FollowMapper;
import com.school.social.service.FollowService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FollowServiceImpl implements FollowService {

    @Resource
    private FollowMapper mapper;

    @Override
    public int insert(Follow entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Follow entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Follow selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Follow> selectAll() {
        return mapper.selectAll();
    }
}

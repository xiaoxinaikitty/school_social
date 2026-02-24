package com.school.social.service.impl;

import com.school.social.entity.Like;
import com.school.social.mapper.LikeMapper;
import com.school.social.service.LikeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LikeServiceImpl implements LikeService {

    @Resource
    private LikeMapper mapper;

    @Override
    public int insert(Like entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Like entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Like selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Like> selectAll() {
        return mapper.selectAll();
    }
}

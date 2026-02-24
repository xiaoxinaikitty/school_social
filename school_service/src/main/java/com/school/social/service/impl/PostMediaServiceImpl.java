package com.school.social.service.impl;

import com.school.social.entity.PostMedia;
import com.school.social.mapper.PostMediaMapper;
import com.school.social.service.PostMediaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PostMediaServiceImpl implements PostMediaService {

    @Resource
    private PostMediaMapper mapper;

    @Override
    public int insert(PostMedia entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(PostMedia entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public PostMedia selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<PostMedia> selectAll() {
        return mapper.selectAll();
    }
}

package com.school.social.service.impl;

import com.school.social.entity.Post;
import com.school.social.mapper.PostMapper;
import com.school.social.service.PostService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper mapper;

    @Override
    public int insert(Post entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Post entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Post selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Post> selectAll() {
        return mapper.selectAll();
    }
}

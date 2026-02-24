package com.school.social.service.impl;

import com.school.social.entity.PostTag;
import com.school.social.mapper.PostTagMapper;
import com.school.social.service.PostTagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PostTagServiceImpl implements PostTagService {

    @Resource
    private PostTagMapper mapper;

    @Override
    public int insert(PostTag entity) {
        return mapper.insert(entity);
    }

    @Override
    public int deleteByPk(Long postId, Long tagId) {
        return mapper.deleteByPk(postId, tagId);
    }

    @Override
    public PostTag selectByPk(Long postId, Long tagId) {
        return mapper.selectByPk(postId, tagId);
    }

    @Override
    public List<PostTag> selectAll() {
        return mapper.selectAll();
    }
}

package com.school.social.service.impl;

import com.school.social.entity.Comment;
import com.school.social.mapper.CommentMapper;
import com.school.social.service.CommentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper mapper;

    @Override
    public int insert(Comment entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Comment entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Comment selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Comment> selectAll() {
        return mapper.selectAll();
    }
}

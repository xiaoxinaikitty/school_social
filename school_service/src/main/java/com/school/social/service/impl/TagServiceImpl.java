package com.school.social.service.impl;

import com.school.social.entity.Tag;
import com.school.social.mapper.TagMapper;
import com.school.social.service.TagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Resource
    private TagMapper mapper;

    @Override
    public int insert(Tag entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Tag entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Tag selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Tag> selectAll() {
        return mapper.selectAll();
    }
}

package com.school.social.service;

import java.util.List;
import com.school.social.entity.Tag;

public interface TagService {
    int insert(Tag entity);
    int updateById(Tag entity);
    int deleteById(Long id);
    Tag selectById(Long id);
    List<Tag> selectAll();
}

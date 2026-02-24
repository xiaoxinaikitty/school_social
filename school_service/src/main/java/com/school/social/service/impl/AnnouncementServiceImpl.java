package com.school.social.service.impl;

import com.school.social.entity.Announcement;
import com.school.social.mapper.AnnouncementMapper;
import com.school.social.service.AnnouncementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Resource
    private AnnouncementMapper mapper;

    @Override
    public int insert(Announcement entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Announcement entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Announcement selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Announcement> selectAll() {
        return mapper.selectAll();
    }
}

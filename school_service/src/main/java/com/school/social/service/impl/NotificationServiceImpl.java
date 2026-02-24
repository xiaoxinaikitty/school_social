package com.school.social.service.impl;

import com.school.social.entity.Notification;
import com.school.social.mapper.NotificationMapper;
import com.school.social.service.NotificationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper mapper;

    @Override
    public int insert(Notification entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Notification entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Notification selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Notification> selectAll() {
        return mapper.selectAll();
    }
}

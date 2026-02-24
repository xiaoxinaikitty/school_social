package com.school.social.service;

import java.util.List;
import com.school.social.entity.Notification;

public interface NotificationService {
    int insert(Notification entity);
    int updateById(Notification entity);
    int deleteById(Long id);
    Notification selectById(Long id);
    List<Notification> selectAll();
}

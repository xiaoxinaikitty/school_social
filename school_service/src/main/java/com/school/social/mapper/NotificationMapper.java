package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Notification;

@Mapper
public interface NotificationMapper {
    int insert(Notification entity);
    int updateById(Notification entity);
    int deleteById(Long id);
    Notification selectById(Long id);
    List<Notification> selectAll();
}

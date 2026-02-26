package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Notification;

@Mapper
public interface NotificationMapper {
    int insert(Notification entity);
    int updateById(Notification entity);
    int deleteById(Long id);
    Notification selectById(Long id);
    List<Notification> selectAll();

    List<Notification> selectByUserPaged(@Param("userId") Long userId,
                                         @Param("isRead") Integer isRead,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    int countByUser(@Param("userId") Long userId,
                    @Param("isRead") Integer isRead);

    int countUnread(@Param("userId") Long userId);

    int markRead(@Param("id") Long id,
                 @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);
}

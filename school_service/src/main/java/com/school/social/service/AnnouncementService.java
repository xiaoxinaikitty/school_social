package com.school.social.service;

import java.util.List;
import com.school.social.entity.Announcement;

public interface AnnouncementService {
    int insert(Announcement entity);
    int updateById(Announcement entity);
    int deleteById(Long id);
    Announcement selectById(Long id);
    List<Announcement> selectAll();
}

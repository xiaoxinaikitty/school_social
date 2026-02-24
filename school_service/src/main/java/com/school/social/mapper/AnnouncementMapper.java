package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Announcement;

@Mapper
public interface AnnouncementMapper {
    int insert(Announcement entity);
    int updateById(Announcement entity);
    int deleteById(Long id);
    Announcement selectById(Long id);
    List<Announcement> selectAll();
}

package com.school.social.service;

import java.util.List;
import com.school.social.entity.UserTag;

public interface UserTagService {
    int insert(UserTag entity);
    int deleteByPk(Long userId, Long tagId);
    UserTag selectByPk(Long userId, Long tagId);
    List<UserTag> selectAll();
}

package com.school.social.service;

import java.util.List;
import com.school.social.entity.BehaviorLog;

public interface BehaviorLogService {
    int insert(BehaviorLog entity);
    int updateById(BehaviorLog entity);
    int deleteById(Long id);
    BehaviorLog selectById(Long id);
    List<BehaviorLog> selectAll();
}

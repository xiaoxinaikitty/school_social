package com.school.social.service.impl;

import com.school.social.entity.BehaviorLog;
import com.school.social.mapper.BehaviorLogMapper;
import com.school.social.service.BehaviorLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BehaviorLogServiceImpl implements BehaviorLogService {

    @Resource
    private BehaviorLogMapper mapper;

    @Override
    public int insert(BehaviorLog entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(BehaviorLog entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public BehaviorLog selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<BehaviorLog> selectAll() {
        return mapper.selectAll();
    }
}

package com.school.social.service.impl;

import com.school.social.entity.RecommendationLog;
import com.school.social.mapper.RecommendationLogMapper;
import com.school.social.service.RecommendationLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RecommendationLogServiceImpl implements RecommendationLogService {

    @Resource
    private RecommendationLogMapper mapper;

    @Override
    public int insert(RecommendationLog entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(RecommendationLog entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public RecommendationLog selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<RecommendationLog> selectAll() {
        return mapper.selectAll();
    }
}

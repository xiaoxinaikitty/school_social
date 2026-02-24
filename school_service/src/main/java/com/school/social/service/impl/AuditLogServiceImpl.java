package com.school.social.service.impl;

import com.school.social.entity.AuditLog;
import com.school.social.mapper.AuditLogMapper;
import com.school.social.service.AuditLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private AuditLogMapper mapper;

    @Override
    public int insert(AuditLog entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(AuditLog entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public AuditLog selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<AuditLog> selectAll() {
        return mapper.selectAll();
    }
}

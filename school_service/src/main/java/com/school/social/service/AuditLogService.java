package com.school.social.service;

import java.util.List;
import com.school.social.entity.AuditLog;

public interface AuditLogService {
    int insert(AuditLog entity);
    int updateById(AuditLog entity);
    int deleteById(Long id);
    AuditLog selectById(Long id);
    List<AuditLog> selectAll();
}

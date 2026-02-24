package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.AuditLog;

@Mapper
public interface AuditLogMapper {
    int insert(AuditLog entity);
    int updateById(AuditLog entity);
    int deleteById(Long id);
    AuditLog selectById(Long id);
    List<AuditLog> selectAll();
}

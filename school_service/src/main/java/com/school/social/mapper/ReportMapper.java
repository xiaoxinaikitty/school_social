package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Report;

@Mapper
public interface ReportMapper {
    int insert(Report entity);
    int updateById(Report entity);
    int deleteById(Long id);
    Report selectById(Long id);
    List<Report> selectAll();
}

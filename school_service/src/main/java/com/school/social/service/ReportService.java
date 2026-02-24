package com.school.social.service;

import java.util.List;
import com.school.social.entity.Report;

public interface ReportService {
    int insert(Report entity);
    int updateById(Report entity);
    int deleteById(Long id);
    Report selectById(Long id);
    List<Report> selectAll();
}

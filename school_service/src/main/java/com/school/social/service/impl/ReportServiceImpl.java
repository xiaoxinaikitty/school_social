package com.school.social.service.impl;

import com.school.social.entity.Report;
import com.school.social.mapper.ReportMapper;
import com.school.social.service.ReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Resource
    private ReportMapper mapper;

    @Override
    public int insert(Report entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Report entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Report selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Report> selectAll() {
        return mapper.selectAll();
    }
}

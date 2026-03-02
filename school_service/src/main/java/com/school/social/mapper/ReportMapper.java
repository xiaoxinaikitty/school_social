package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Report;

@Mapper
public interface ReportMapper {
    int insert(Report entity);
    int updateById(Report entity);
    int deleteById(Long id);
    Report selectById(Long id);
    List<Report> selectAll();

    List<Report> selectByReporterPaged(@Param("reporterId") Long reporterId,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    int countByReporter(@Param("reporterId") Long reporterId,
                        @Param("status") Integer status);

    List<Report> selectAllPaged(@Param("status") Integer status,
                                @Param("offset") int offset,
                                @Param("size") int size);

    int countAll(@Param("status") Integer status);
}

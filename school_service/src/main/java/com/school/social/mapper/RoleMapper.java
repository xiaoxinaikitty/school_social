package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Role;

@Mapper
public interface RoleMapper {
    int insert(Role entity);
    int updateById(Role entity);
    int deleteById(Long id);
    Role selectById(Long id);
    List<Role> selectAll();
}

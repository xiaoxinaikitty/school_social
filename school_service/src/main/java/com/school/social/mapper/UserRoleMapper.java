package com.school.social.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.UserRole;

@Mapper
public interface UserRoleMapper {
    int insert(UserRole entity);
    int deleteByPk(@Param("userId") Long userId, @Param("roleId") Long roleId);
    UserRole selectByPk(@Param("userId") Long userId, @Param("roleId") Long roleId);
    List<UserRole> selectAll();
}

package com.school.social.mapper;

import com.school.social.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User entity);
    int updateById(User entity);
    int deleteById(Long id);
    User selectById(Long id);
    List<User> selectAll();

    User selectByUsername(@Param("username") String username);
    User selectByEmail(@Param("email") String email);
    User selectByPhone(@Param("phone") String phone);
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    List<User> selectPaged(@Param("keyword") String keyword,
                           @Param("status") Integer status,
                           @Param("offset") int offset,
                           @Param("size") int size);
    int countByCondition(@Param("keyword") String keyword, @Param("status") Integer status);
}


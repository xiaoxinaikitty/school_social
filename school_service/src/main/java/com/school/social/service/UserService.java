package com.school.social.service;

import java.util.List;
import com.school.social.entity.User;

public interface UserService {
    int insert(User entity);
    int updateById(User entity);
    int deleteById(Long id);
    User selectById(Long id);
    List<User> selectAll();
}

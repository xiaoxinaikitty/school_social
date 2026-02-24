package com.school.social.service;

import java.util.List;
import com.school.social.entity.Favorite;

public interface FavoriteService {
    int insert(Favorite entity);
    int updateById(Favorite entity);
    int deleteById(Long id);
    Favorite selectById(Long id);
    List<Favorite> selectAll();
}

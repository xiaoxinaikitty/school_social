package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.school.social.entity.Favorite;

@Mapper
public interface FavoriteMapper {
    int insert(Favorite entity);
    int updateById(Favorite entity);
    int deleteById(Long id);
    Favorite selectById(Long id);
    List<Favorite> selectAll();
}

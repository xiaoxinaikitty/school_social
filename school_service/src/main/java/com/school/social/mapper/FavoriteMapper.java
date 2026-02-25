package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Favorite;
import com.school.social.entity.Post;

@Mapper
public interface FavoriteMapper {
    int insert(Favorite entity);
    int updateById(Favorite entity);
    int deleteById(Long id);
    Favorite selectById(Long id);
    List<Favorite> selectAll();

    List<Post> selectFavoritePostsByUserId(@Param("userId") Long userId);

    List<Post> selectFavoritePostsByUserIdPaged(@Param("userId") Long userId,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    int countFavoritePostsByUserId(@Param("userId") Long userId);
}

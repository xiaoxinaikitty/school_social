package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Follow;
import com.school.social.dto.auth.UserView;

@Mapper
public interface FollowMapper {
    int insert(Follow entity);
    int updateById(Follow entity);
    int deleteById(Long id);
    Follow selectById(Long id);
    List<Follow> selectAll();

    Follow selectByFollowerAndFollowee(@Param("followerId") Long followerId,
                                       @Param("followeeId") Long followeeId);

    int deleteByFollowerAndFollowee(@Param("followerId") Long followerId,
                                    @Param("followeeId") Long followeeId);

    int countFollowers(@Param("userId") Long userId);

    int countFollowing(@Param("userId") Long userId);

    List<UserView> selectFollowersPaged(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    List<UserView> selectFollowingPaged(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);
}

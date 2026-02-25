package com.school.social.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.school.social.entity.Post;

@Mapper
public interface PostMapper {
    int insert(Post entity);
    int updateById(Post entity);
    int deleteById(Long id);
    Post selectById(Long id);
    List<Post> selectAll();

    List<Post> selectByUserId(@Param("userId") Long userId);

    List<Post> selectByUserIdPaged(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    int countByUserId(@Param("userId") Long userId);

    List<Post> selectPaged(@Param("status") Integer status,
                           @Param("offset") int offset,
                           @Param("size") int size);

    int countByStatus(@Param("status") Integer status);

    List<Post> selectRecommendPaged(@Param("offset") int offset,
                                    @Param("size") int size);

    int countRecommend();

    List<Post> selectLatestPaged(@Param("offset") int offset,
                                 @Param("size") int size);

    int countLatest();

    List<Post> selectHotPaged(@Param("offset") int offset,
                              @Param("size") int size);

    int countHot();

    List<Post> selectByTagPaged(@Param("tagId") Long tagId,
                                @Param("offset") int offset,
                                @Param("size") int size);

    int countByTag(@Param("tagId") Long tagId);

    List<Post> selectByFolloweePaged(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    int countByFollowee(@Param("userId") Long userId);

    List<Post> searchPaged(@Param("keyword") String keyword,
                           @Param("tagId") Long tagId,
                           @Param("offset") int offset,
                           @Param("size") int size);

    int countSearch(@Param("keyword") String keyword,
                    @Param("tagId") Long tagId);

    List<Post> selectRelatedByTagIds(@Param("tagIds") List<Long> tagIds,
                                     @Param("excludeId") Long excludeId,
                                     @Param("limit") int limit);
}

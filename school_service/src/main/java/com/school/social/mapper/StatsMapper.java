package com.school.social.mapper;

import com.school.social.dto.admin.RetentionCount;
import com.school.social.dto.admin.StatCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatsMapper {
    int countUsers();
    int countPosts();
    int countLikes();
    int countComments();
    int countFavorites();
    int countShares();

    int countUsersSince(@Param("start") LocalDateTime start);
    int countPostsSince(@Param("start") LocalDateTime start);
    int countLikesSince(@Param("start") LocalDateTime start);
    int countCommentsSince(@Param("start") LocalDateTime start);
    int countFavoritesSince(@Param("start") LocalDateTime start);
    int countSharesSince(@Param("start") LocalDateTime start);

    int countActiveUsersSince(@Param("start") LocalDateTime start);

    List<StatCount> selectNewUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<StatCount> selectActiveUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<RetentionCount> selectRetention1d(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<RetentionCount> selectRetention7d(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<StatCount> selectPosts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<StatCount> selectLikes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<StatCount> selectComments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<StatCount> selectFavorites(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<StatCount> selectShares(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

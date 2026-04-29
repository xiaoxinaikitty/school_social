package com.school.social.mapper;

import com.school.social.entity.PasswordResetCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PasswordResetCodeMapper {
    int insert(PasswordResetCode entity);

    PasswordResetCode selectLatestActiveByEmailAndScene(@Param("email") String email, @Param("scene") String scene);

    int invalidateActiveByEmailAndScene(@Param("email") String email,
                                        @Param("scene") String scene,
                                        @Param("status") Integer status,
                                        @Param("updatedAt") LocalDateTime updatedAt);

    int markUsed(@Param("id") Long id,
                 @Param("usedAt") LocalDateTime usedAt,
                 @Param("updatedAt") LocalDateTime updatedAt);

    int incrementAttemptCount(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    int countByEmailSince(@Param("email") String email,
                          @Param("scene") String scene,
                          @Param("createdAt") LocalDateTime createdAt);

    int countByIpSince(@Param("requestIp") String requestIp,
                       @Param("scene") String scene,
                       @Param("createdAt") LocalDateTime createdAt);
}

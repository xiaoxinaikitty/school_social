package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.dto.admin.AdminContentStat;
import com.school.social.dto.admin.AdminOverviewStat;
import com.school.social.dto.admin.AdminUserStat;
import com.school.social.dto.admin.RetentionCount;
import com.school.social.dto.admin.StatCount;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.StatsMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {
    private static final int MAX_DAYS = 180;

    @Resource
    private StatsMapper statsMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewStat> overview(@RequestParam(defaultValue = "30") int days,
                                                   HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safeDays = clampDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();

        AdminOverviewStat resp = new AdminOverviewStat();
        resp.setRangeStart(startDate.toString());
        resp.setRangeEnd(endDate.toString());
        resp.setTotalUsers(statsMapper.countUsers());
        resp.setTotalPosts(statsMapper.countPosts());
        int totalLikes = statsMapper.countLikes();
        int totalComments = statsMapper.countComments();
        int totalFavorites = statsMapper.countFavorites();
        int totalShares = statsMapper.countShares();
        resp.setTotalInteractions(totalLikes + totalComments + totalFavorites + totalShares);

        resp.setNewUsers(statsMapper.countUsersSince(start));
        resp.setActiveUsers(statsMapper.countActiveUsersSince(start));
        resp.setPostCount(statsMapper.countPostsSince(start));

        int likesSince = statsMapper.countLikesSince(start);
        int commentsSince = statsMapper.countCommentsSince(start);
        int favoritesSince = statsMapper.countFavoritesSince(start);
        int sharesSince = statsMapper.countSharesSince(start);
        resp.setInteractionCount(likesSince + commentsSince + favoritesSince + sharesSince);

        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        Map<LocalDate, Integer> newUsersMap = toDateCountMap(statsMapper.selectNewUsers(start, endExclusive));
        Map<LocalDate, Integer> retention1dMap = toRetentionMap(statsMapper.selectRetention1d(start, endExclusive));
        Map<LocalDate, Integer> retention7dMap = toRetentionMap(statsMapper.selectRetention7d(start, endExclusive));

        resp.setRetention1d(calcRetention(endDate, newUsersMap, retention1dMap, 1));
        resp.setRetention7d(calcRetention(endDate, newUsersMap, retention7dMap, 7));

        return ApiResponse.success(resp);
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserStat>> userStats(@RequestParam(defaultValue = "30") int days,
                                                      HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safeDays = clampDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Integer> newUsersMap = toDateCountMap(statsMapper.selectNewUsers(start, endExclusive));
        Map<LocalDate, Integer> activeUsersMap = toDateCountMap(statsMapper.selectActiveUsers(start, endExclusive));
        Map<LocalDate, Integer> retention1dMap = toRetentionMap(statsMapper.selectRetention1d(start, endExclusive));
        Map<LocalDate, Integer> retention7dMap = toRetentionMap(statsMapper.selectRetention7d(start, endExclusive));

        List<AdminUserStat> list = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            AdminUserStat item = new AdminUserStat();
            item.setDate(cursor.toString());
            item.setNewUsers(newUsersMap.getOrDefault(cursor, 0));
            item.setActiveUsers(activeUsersMap.getOrDefault(cursor, 0));
            item.setRetention1d(calcRetention(cursor, newUsersMap, retention1dMap, 1));
            item.setRetention7d(calcRetention(cursor, newUsersMap, retention7dMap, 7));
            list.add(item);
            cursor = cursor.plusDays(1);
        }
        return ApiResponse.success(list);
    }

    @GetMapping("/content")
    public ApiResponse<List<AdminContentStat>> contentStats(@RequestParam(defaultValue = "30") int days,
                                                            HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return ApiResponse.fail("无权限访问");
        }
        int safeDays = clampDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Integer> postMap = toDateCountMap(statsMapper.selectPosts(start, endExclusive));
        Map<LocalDate, Integer> likeMap = toDateCountMap(statsMapper.selectLikes(start, endExclusive));
        Map<LocalDate, Integer> commentMap = toDateCountMap(statsMapper.selectComments(start, endExclusive));
        Map<LocalDate, Integer> favoriteMap = toDateCountMap(statsMapper.selectFavorites(start, endExclusive));
        Map<LocalDate, Integer> shareMap = toDateCountMap(statsMapper.selectShares(start, endExclusive));

        List<AdminContentStat> list = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            AdminContentStat item = new AdminContentStat();
            item.setDate(cursor.toString());
            int posts = postMap.getOrDefault(cursor, 0);
            int likes = likeMap.getOrDefault(cursor, 0);
            int comments = commentMap.getOrDefault(cursor, 0);
            int favorites = favoriteMap.getOrDefault(cursor, 0);
            int shares = shareMap.getOrDefault(cursor, 0);
            item.setPostCount(posts);
            item.setLikeCount(likes);
            item.setCommentCount(comments);
            item.setFavoriteCount(favorites);
            item.setShareCount(shares);
            item.setInteractionCount(likes + comments + favorites + shares);
            list.add(item);
            cursor = cursor.plusDays(1);
        }
        return ApiResponse.success(list);
    }

    private int clampDays(int days) {
        if (days <= 0) {
            return 30;
        }
        return Math.min(days, MAX_DAYS);
    }

    private Map<LocalDate, Integer> toDateCountMap(List<StatCount> list) {
        Map<LocalDate, Integer> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (StatCount item : list) {
            if (item == null || item.getStatDate() == null) {
                continue;
            }
            LocalDate date = LocalDate.parse(item.getStatDate());
            map.put(date, item.getCount() == null ? 0 : item.getCount());
        }
        return map;
    }

    private Map<LocalDate, Integer> toRetentionMap(List<RetentionCount> list) {
        Map<LocalDate, Integer> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (RetentionCount item : list) {
            if (item == null || item.getStatDate() == null) {
                continue;
            }
            LocalDate date = LocalDate.parse(item.getStatDate());
            map.put(date, item.getRetained() == null ? 0 : item.getRetained());
        }
        return map;
    }

    private double calcRetention(LocalDate date,
                                 Map<LocalDate, Integer> newUsersMap,
                                 Map<LocalDate, Integer> retentionMap,
                                 int offsetDays) {
        if (date == null) {
            return 0.0;
        }
        LocalDate cohortDate = date.minusDays(offsetDays);
        int cohortSize = newUsersMap.getOrDefault(cohortDate, 0);
        if (cohortSize <= 0) {
            return 0.0;
        }
        int retained = retentionMap.getOrDefault(date, 0);
        return retained * 1.0 / cohortSize;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return false;
        }
        Role role = roleMapper.selectByName("admin");
        if (role == null) {
            return false;
        }
        UserRole link = userRoleMapper.selectByPk(userId, role.getId());
        return link != null;
    }
}

package com.school.social.service.impl;

import com.school.social.common.PageResponse;
import com.school.social.config.RecommendConfigStore;
import com.school.social.dto.admin.RecommendConfig;
import com.school.social.dto.admin.RecommendOverview;
import com.school.social.dto.admin.RecommendPostStat;
import com.school.social.dto.admin.RecommendSceneStat;
import com.school.social.dto.user.UserTagView;
import com.school.social.entity.BehaviorLog;
import com.school.social.entity.Post;
import com.school.social.entity.RecommendationLog;
import com.school.social.mapper.BehaviorLogMapper;
import com.school.social.mapper.FollowMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.PostTagMapper;
import com.school.social.mapper.RecommendationLogMapper;
import com.school.social.mapper.UserTagMapper;
import com.school.social.service.RecommendationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {
    private static final int SCENE_RECOMMEND = 0;
    private static final int ACTION_VIEW = 0;
    private static final int ACTION_RECOMMEND_CLICK = 5;
    private static final int TARGET_POST = 0;

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostTagMapper postTagMapper;

    @Resource
    private UserTagMapper userTagMapper;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private BehaviorLogMapper behaviorLogMapper;

    @Resource
    private RecommendationLogMapper recommendationLogMapper;

    @Resource
    private RecommendConfigStore recommendConfigStore;

    @Override
    public PageResponse<Post> recommend(Long userId, int page, int size) {
        List<Post> source = postMapper.selectApprovedAll();
        if (source == null || source.isEmpty()) {
            return new PageResponse<>(page, size, 0, Collections.emptyList());
        }

        RecommendConfig config = recommendConfigStore.get();
        Map<Long, Double> preferenceTags = buildPreferenceTags(userId);
        List<Long> followeeList = userId == null ? Collections.emptyList() : followMapper.selectFolloweeIds(userId);
        Set<Long> followeeIds = followeeList == null ? Collections.emptySet() : new HashSet<>(followeeList);

        List<PostScore> scored = new ArrayList<>();
        double maxHot = 0;
        double maxQuality = 0;
        for (Post post : source) {
            if (post == null || post.getStatus() == null || post.getStatus() != 1) {
                continue;
            }
            if (userId != null && userId.equals(post.getUserId())) {
                continue;
            }
            double hotValue = calcHotValue(post);
            double qualityValue = post.getQualityScore() == null ? 0 : post.getQualityScore().doubleValue();
            maxHot = Math.max(maxHot, hotValue);
            maxQuality = Math.max(maxQuality, qualityValue);
            scored.add(new PostScore(post, hotValue, qualityValue, postTagMapper.selectTagIdsByPostId(post.getId())));
        }

        final double finalMaxHot = maxHot <= 0 ? 1 : maxHot;
        final double finalMaxQuality = maxQuality <= 0 ? 1 : maxQuality;
        for (PostScore item : scored) {
            item.score = calcFinalScore(item, config, preferenceTags, followeeIds, finalMaxHot, finalMaxQuality);
        }

        scored.sort(Comparator
            .comparing(PostScore::getScore).reversed()
            .thenComparing(PostScore::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PostScore::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        int total = scored.size();
        int fromIndex = Math.max((page - 1) * size, 0);
        if (fromIndex >= total) {
            return new PageResponse<>(page, size, total, Collections.emptyList());
        }
        int toIndex = Math.min(fromIndex + size, total);
        List<PostScore> pageScores = scored.subList(fromIndex, toIndex);
        if (userId != null) {
            logExposures(userId, pageScores, fromIndex);
        }
        List<Post> result = pageScores.stream().map(item -> item.post).collect(Collectors.toList());
        return new PageResponse<>(page, size, total, result);
    }

    @Override
    public void recordClick(Long userId, Long postId, Integer scene) {
        if (userId == null || postId == null) {
            return;
        }
        int resolvedScene = scene == null ? SCENE_RECOMMEND : scene;
        RecommendationLog latest = recommendationLogMapper.selectLatestByUserAndPostScene(userId, postId, resolvedScene);
        if (latest != null) {
            RecommendationLog update = new RecommendationLog();
            update.setId(latest.getId());
            update.setIsClicked(1);
            recommendationLogMapper.updateById(update);
        } else {
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setPostId(postId);
            log.setScene(resolvedScene);
            log.setScore(BigDecimal.ZERO);
            log.setRankPos(0);
            log.setIsClicked(1);
            log.setCreatedAt(LocalDateTime.now());
            recommendationLogMapper.insert(log);
        }
        insertBehavior(userId, ACTION_RECOMMEND_CLICK, postId, 0, BigDecimal.valueOf(1.20));
    }

    @Override
    public void recordDetailView(Long userId, Long postId) {
        if (postId == null) {
            return;
        }
        postMapper.increaseViewCount(postId);
        if (userId != null) {
            insertBehavior(userId, ACTION_VIEW, postId, 8, BigDecimal.valueOf(0.80));
        }
    }

    @Override
    public RecommendOverview getOverview(int days) {
        int safeDays = Math.max(days, 1);
        LocalDateTime start = LocalDateTime.now().minusDays(safeDays);
        int exposures = recommendationLogMapper.countSince(start);
        int clicks = recommendationLogMapper.countClicksSince(start);
        RecommendOverview overview = new RecommendOverview();
        overview.setExposureCount(exposures);
        overview.setClickCount(clicks);
        overview.setClickRate(calcRate(clicks, exposures));
        List<RecommendSceneStat> scenes = recommendationLogMapper.selectSceneStatsSince(start);
        if (scenes != null) {
            for (RecommendSceneStat scene : scenes) {
                scene.setClickRate(calcRate(scene.getClickCount(), scene.getExposureCount()));
            }
        }
        List<RecommendPostStat> posts = recommendationLogMapper.selectTopPostsSince(start, 8);
        if (posts != null) {
            for (RecommendPostStat post : posts) {
                post.setClickRate(calcRate(post.getClickCount(), post.getExposureCount()));
            }
        }
        overview.setScenes(scenes);
        overview.setTopPosts(posts);
        return overview;
    }

    private Map<Long, Double> buildPreferenceTags(Long userId) {
        Map<Long, Double> weights = new HashMap<>();
        if (userId == null) {
            return weights;
        }
        List<UserTagView> userTags = userTagMapper.selectByUserId(userId);
        if (userTags != null) {
            for (UserTagView userTag : userTags) {
                if (userTag.getTagId() == null) {
                    continue;
                }
                double baseWeight = userTag.getWeight() == null ? 1.0 : userTag.getWeight().doubleValue();
                weights.put(userTag.getTagId(), weights.getOrDefault(userTag.getTagId(), 0D) + baseWeight);
            }
        }

        List<BehaviorLog> recentLogs = behaviorLogMapper.selectRecentByUser(userId, 50);
        if (recentLogs != null) {
            for (BehaviorLog log : recentLogs) {
                if (log == null || log.getTargetType() == null || log.getTargetType() != TARGET_POST || log.getTargetId() == null) {
                    continue;
                }
                List<Long> tagIds = postTagMapper.selectTagIdsByPostId(log.getTargetId());
                if (tagIds == null || tagIds.isEmpty()) {
                    continue;
                }
                double actionWeight = resolveActionWeight(log);
                for (Long tagId : tagIds) {
                    if (tagId == null) {
                        continue;
                    }
                    weights.put(tagId, weights.getOrDefault(tagId, 0D) + actionWeight);
                }
            }
        }
        return weights;
    }

    private double calcFinalScore(PostScore item, RecommendConfig config, Map<Long, Double> preferenceTags,
                                  Set<Long> followeeIds, double maxHot, double maxQuality) {
        double hotScore = (config.getEnableHot() != null && config.getEnableHot()) ? item.hotValue / maxHot : 0;
        double timeScore = calcTimeScore(item.post.getPublishedAt() != null ? item.post.getPublishedAt() : item.post.getCreatedAt());
        double qualityScore = item.qualityValue / maxQuality;
        double tagScore = (config.getEnableTag() != null && config.getEnableTag()) ? calcTagScore(item.tagIds, preferenceTags) : 0;
        double followScore = (config.getEnableFollow() != null && config.getEnableFollow() && followeeIds.contains(item.post.getUserId())) ? 1 : 0;

        double hotWeight = (config.getEnableHot() != null && config.getEnableHot()) ? defaultWeight(config.getWeightHot(), 0.4) : 0;
        double timeWeight = defaultWeight(config.getWeightTime(), 0.2);
        double qualityWeight = defaultWeight(config.getWeightQuality(), 0.2);
        double tagWeight = (config.getEnableTag() != null && config.getEnableTag()) ? defaultWeight(config.getWeightTag(), 0.1) : 0;
        double followWeight = (config.getEnableFollow() != null && config.getEnableFollow()) ? defaultWeight(config.getWeightFollow(), 0.1) : 0;
        double totalWeight = hotWeight + timeWeight + qualityWeight + tagWeight + followWeight;
        if (totalWeight <= 0) {
            totalWeight = 1;
        }
        double weighted = hotScore * hotWeight
            + timeScore * timeWeight
            + qualityScore * qualityWeight
            + tagScore * tagWeight
            + followScore * followWeight;
        double pinnedBoost = item.post.getIsPinned() != null && item.post.getIsPinned() == 1 ? 0.08 : 0;
        double featuredBoost = item.post.getIsFeatured() != null && item.post.getIsFeatured() == 1 ? 0.08 : 0;
        return weighted / totalWeight + pinnedBoost + featuredBoost;
    }

    private double calcHotValue(Post post) {
        double likes = post.getLikeCount() == null ? 0 : post.getLikeCount();
        double comments = post.getCommentCount() == null ? 0 : post.getCommentCount();
        double favorites = post.getFavoriteCount() == null ? 0 : post.getFavoriteCount();
        double views = post.getViewCount() == null ? 0 : post.getViewCount();
        return likes * 3 + comments * 4 + favorites * 3 + views * 0.08;
    }

    private double calcTimeScore(LocalDateTime time) {
        if (time == null) {
            return 0;
        }
        long hours = Math.max(Duration.between(time, LocalDateTime.now()).toHours(), 0);
        return 1D / (1D + (hours / 24D));
    }

    private double calcTagScore(List<Long> postTagIds, Map<Long, Double> preferenceTags) {
        if (postTagIds == null || postTagIds.isEmpty() || preferenceTags.isEmpty()) {
            return 0;
        }
        double total = 0;
        double matched = 0;
        for (Double value : preferenceTags.values()) {
            total += value == null ? 0 : value;
        }
        if (total <= 0) {
            return 0;
        }
        for (Long tagId : postTagIds) {
            if (tagId == null) {
                continue;
            }
            matched += preferenceTags.getOrDefault(tagId, 0D);
        }
        return Math.min(matched / total, 1D);
    }

    private double resolveActionWeight(BehaviorLog log) {
        double base = log.getWeight() == null ? 1.0 : log.getWeight().doubleValue();
        if (log.getActionType() == null) {
            return base;
        }
        if (log.getActionType() == ACTION_RECOMMEND_CLICK) {
            return base * 1.4;
        }
        if (log.getActionType() == ACTION_VIEW) {
            return base * 1.0;
        }
        if (log.getActionType() == 4) {
            return base * 1.6;
        }
        return base;
    }

    private void logExposures(Long userId, List<PostScore> pageScores, int offset) {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < pageScores.size(); index++) {
            PostScore item = pageScores.get(index);
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setPostId(item.post.getId());
            log.setScene(SCENE_RECOMMEND);
            log.setScore(BigDecimal.valueOf(item.score).setScale(4, RoundingMode.HALF_UP));
            log.setRankPos(offset + index + 1);
            log.setIsClicked(0);
            log.setCreatedAt(now);
            recommendationLogMapper.insert(log);
        }
    }

    private void insertBehavior(Long userId, int actionType, Long postId, int durationSec, BigDecimal weight) {
        BehaviorLog log = new BehaviorLog();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setTargetType(TARGET_POST);
        log.setTargetId(postId);
        log.setDurationSec(durationSec);
        log.setWeight(weight);
        log.setCreatedAt(LocalDateTime.now());
        behaviorLogMapper.insert(log);
    }

    private double defaultWeight(Double actual, double fallback) {
        return actual == null ? fallback : actual;
    }

    private Double calcRate(Integer numerator, Integer denominator) {
        int safeNumerator = numerator == null ? 0 : numerator;
        int safeDenominator = denominator == null ? 0 : denominator;
        if (safeDenominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf((double) safeNumerator / safeDenominator)
            .setScale(4, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private static class PostScore {
        private final Post post;
        private final double hotValue;
        private final double qualityValue;
        private final List<Long> tagIds;
        private double score;

        private PostScore(Post post, double hotValue, double qualityValue, List<Long> tagIds) {
            this.post = post;
            this.hotValue = hotValue;
            this.qualityValue = qualityValue;
            this.tagIds = tagIds == null ? Collections.emptyList() : tagIds;
        }

        public double getScore() {
            return score;
        }

        public LocalDateTime getCreatedAt() {
            return post.getCreatedAt();
        }

        public Long getId() {
            return post.getId();
        }
    }
}

package com.school.social.config;

import com.school.social.dto.admin.RecommendConfig;
import com.school.social.entity.RecommendConfigEntity;
import com.school.social.mapper.RecommendConfigMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class RecommendConfigStore {
    private static final long CONFIG_ID = 1L;

    @Resource
    private RecommendConfigMapper recommendConfigMapper;

    public RecommendConfig get() {
        RecommendConfigEntity entity = recommendConfigMapper.selectById(CONFIG_ID);
        if (entity == null) {
            entity = createDefaults();
            recommendConfigMapper.insert(entity);
        }
        return toDto(entity);
    }

    public RecommendConfig update(RecommendConfig incoming) {
        RecommendConfigEntity entity = recommendConfigMapper.selectById(CONFIG_ID);
        if (entity == null) {
            entity = createDefaults();
            recommendConfigMapper.insert(entity);
        }
        RecommendConfigEntity update = new RecommendConfigEntity();
        update.setId(CONFIG_ID);
        update.setEnableHot(resolveBoolean(incoming, entity.getEnableHot(), incoming != null ? incoming.getEnableHot() : null));
        update.setEnableFollow(resolveBoolean(incoming, entity.getEnableFollow(), incoming != null ? incoming.getEnableFollow() : null));
        update.setEnableTag(resolveBoolean(incoming, entity.getEnableTag(), incoming != null ? incoming.getEnableTag() : null));
        update.setWeightHot(resolveDouble(incoming != null ? incoming.getWeightHot() : null, entity.getWeightHot()));
        update.setWeightTime(resolveDouble(incoming != null ? incoming.getWeightTime() : null, entity.getWeightTime()));
        update.setWeightQuality(resolveDouble(incoming != null ? incoming.getWeightQuality() : null, entity.getWeightQuality()));
        update.setWeightTag(resolveDouble(incoming != null ? incoming.getWeightTag() : null, entity.getWeightTag()));
        update.setWeightFollow(resolveDouble(incoming != null ? incoming.getWeightFollow() : null, entity.getWeightFollow()));
        update.setUpdatedAt(LocalDateTime.now());
        recommendConfigMapper.updateById(update);
        RecommendConfigEntity refreshed = recommendConfigMapper.selectById(CONFIG_ID);
        return toDto(refreshed == null ? entity : refreshed);
    }

    private RecommendConfigEntity createDefaults() {
        RecommendConfigEntity defaults = new RecommendConfigEntity();
        defaults.setId(CONFIG_ID);
        defaults.setEnableHot(1);
        defaults.setEnableFollow(1);
        defaults.setEnableTag(1);
        defaults.setWeightHot(0.4);
        defaults.setWeightTime(0.2);
        defaults.setWeightQuality(0.2);
        defaults.setWeightTag(0.1);
        defaults.setWeightFollow(0.1);
        defaults.setUpdatedAt(LocalDateTime.now());
        return defaults;
    }

    private RecommendConfig toDto(RecommendConfigEntity entity) {
        RecommendConfig dto = new RecommendConfig();
        if (entity == null) {
            return dto;
        }
        dto.setEnableHot(entity.getEnableHot() != null && entity.getEnableHot() == 1);
        dto.setEnableFollow(entity.getEnableFollow() != null && entity.getEnableFollow() == 1);
        dto.setEnableTag(entity.getEnableTag() != null && entity.getEnableTag() == 1);
        dto.setWeightHot(entity.getWeightHot());
        dto.setWeightTime(entity.getWeightTime());
        dto.setWeightQuality(entity.getWeightQuality());
        dto.setWeightTag(entity.getWeightTag());
        dto.setWeightFollow(entity.getWeightFollow());
        return dto;
    }

    private Integer resolveBoolean(RecommendConfig incoming, Integer current, Boolean requested) {
        if (incoming == null || requested == null) {
            return current;
        }
        return requested ? 1 : 0;
    }

    private Double resolveDouble(Double requested, Double current) {
        return requested != null ? requested : current;
    }
}

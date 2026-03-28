package com.school.social.dto.admin;

import java.util.List;

public class RecommendOverview {
    private Integer exposureCount;
    private Integer clickCount;
    private Double clickRate;
    private List<RecommendSceneStat> scenes;
    private List<RecommendPostStat> topPosts;

    public Integer getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(Integer exposureCount) {
        this.exposureCount = exposureCount;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Double getClickRate() {
        return clickRate;
    }

    public void setClickRate(Double clickRate) {
        this.clickRate = clickRate;
    }

    public List<RecommendSceneStat> getScenes() {
        return scenes;
    }

    public void setScenes(List<RecommendSceneStat> scenes) {
        this.scenes = scenes;
    }

    public List<RecommendPostStat> getTopPosts() {
        return topPosts;
    }

    public void setTopPosts(List<RecommendPostStat> topPosts) {
        this.topPosts = topPosts;
    }
}

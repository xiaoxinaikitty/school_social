package com.school.social.dto.admin;

public class RecommendPostStat {
    private Long postId;
    private String title;
    private Integer exposureCount;
    private Integer clickCount;
    private Double clickRate;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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
}

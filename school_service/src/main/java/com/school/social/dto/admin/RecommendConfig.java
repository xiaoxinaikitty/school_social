package com.school.social.dto.admin;

public class RecommendConfig {
    private Boolean enableHot;
    private Boolean enableFollow;
    private Boolean enableTag;
    private Double weightHot;
    private Double weightTime;
    private Double weightQuality;
    private Double weightTag;
    private Double weightFollow;

    public Boolean getEnableHot() {
        return enableHot;
    }

    public void setEnableHot(Boolean enableHot) {
        this.enableHot = enableHot;
    }

    public Boolean getEnableFollow() {
        return enableFollow;
    }

    public void setEnableFollow(Boolean enableFollow) {
        this.enableFollow = enableFollow;
    }

    public Boolean getEnableTag() {
        return enableTag;
    }

    public void setEnableTag(Boolean enableTag) {
        this.enableTag = enableTag;
    }

    public Double getWeightHot() {
        return weightHot;
    }

    public void setWeightHot(Double weightHot) {
        this.weightHot = weightHot;
    }

    public Double getWeightTime() {
        return weightTime;
    }

    public void setWeightTime(Double weightTime) {
        this.weightTime = weightTime;
    }

    public Double getWeightQuality() {
        return weightQuality;
    }

    public void setWeightQuality(Double weightQuality) {
        this.weightQuality = weightQuality;
    }

    public Double getWeightTag() {
        return weightTag;
    }

    public void setWeightTag(Double weightTag) {
        this.weightTag = weightTag;
    }

    public Double getWeightFollow() {
        return weightFollow;
    }

    public void setWeightFollow(Double weightFollow) {
        this.weightFollow = weightFollow;
    }
}

package com.school.social.dto.interaction;

public class FollowStatsResponse {
    private Integer followingCount;
    private Integer followerCount;

    public FollowStatsResponse() {
    }

    public FollowStatsResponse(Integer followingCount, Integer followerCount) {
        this.followingCount = followingCount;
        this.followerCount = followerCount;
    }

    public Integer getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Integer followingCount) {
        this.followingCount = followingCount;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }
}

package com.school.social.dto.admin;

public class AdminOverviewStat {
    private String rangeStart;
    private String rangeEnd;
    private Integer totalUsers;
    private Integer totalPosts;
    private Integer totalInteractions;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer postCount;
    private Integer interactionCount;
    private Double retention1d;
    private Double retention7d;

    public String getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(String rangeStart) {
        this.rangeStart = rangeStart;
    }

    public String getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(String rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Integer totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Integer getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(Integer totalInteractions) {
        this.totalInteractions = totalInteractions;
    }

    public Integer getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(Integer newUsers) {
        this.newUsers = newUsers;
    }

    public Integer getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Integer activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public Integer getInteractionCount() {
        return interactionCount;
    }

    public void setInteractionCount(Integer interactionCount) {
        this.interactionCount = interactionCount;
    }

    public Double getRetention1d() {
        return retention1d;
    }

    public void setRetention1d(Double retention1d) {
        this.retention1d = retention1d;
    }

    public Double getRetention7d() {
        return retention7d;
    }

    public void setRetention7d(Double retention7d) {
        this.retention7d = retention7d;
    }
}

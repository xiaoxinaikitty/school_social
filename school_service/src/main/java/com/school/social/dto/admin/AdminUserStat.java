package com.school.social.dto.admin;

public class AdminUserStat {
    private String date;
    private Integer newUsers;
    private Integer activeUsers;
    private Double retention1d;
    private Double retention7d;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

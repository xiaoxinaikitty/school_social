package com.school.social.dto.admin;

public class PostFlagRequest {
    private Integer pinned;
    private Integer featured;

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public Integer getFeatured() {
        return featured;
    }

    public void setFeatured(Integer featured) {
        this.featured = featured;
    }
}

package com.school.social.dto.friend;

public class FriendCandidateView {
    private Long id;
    private String username;
    private String avatarUrl;
    private String school;
    private String college;
    private String bio;
    private String relationStatus;
    private Long pendingRequestId;
    private Long directRoomId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRelationStatus() {
        return relationStatus;
    }

    public void setRelationStatus(String relationStatus) {
        this.relationStatus = relationStatus;
    }

    public Long getPendingRequestId() {
        return pendingRequestId;
    }

    public void setPendingRequestId(Long pendingRequestId) {
        this.pendingRequestId = pendingRequestId;
    }

    public Long getDirectRoomId() {
        return directRoomId;
    }

    public void setDirectRoomId(Long directRoomId) {
        this.directRoomId = directRoomId;
    }
}

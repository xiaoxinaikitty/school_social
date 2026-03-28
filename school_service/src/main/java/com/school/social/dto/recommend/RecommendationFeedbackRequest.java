package com.school.social.dto.recommend;

public class RecommendationFeedbackRequest {
    private Long postId;
    private Integer scene;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Integer getScene() {
        return scene;
    }

    public void setScene(Integer scene) {
        this.scene = scene;
    }
}

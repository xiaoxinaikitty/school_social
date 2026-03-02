package com.school.social.dto.admin;

public class PostReviewRequest {
    private Integer decision;
    private String note;

    public Integer getDecision() {
        return decision;
    }

    public void setDecision(Integer decision) {
        this.decision = decision;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

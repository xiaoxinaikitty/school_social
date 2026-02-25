package com.school.social.dto.user;

import java.util.List;

public class UserTagUpdateRequest {
    private List<Long> tagIds;

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}

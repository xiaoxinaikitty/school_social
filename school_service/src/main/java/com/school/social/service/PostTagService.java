package com.school.social.service;

import java.util.List;
import com.school.social.entity.PostTag;

public interface PostTagService {
    int insert(PostTag entity);
    int deleteByPk(Long postId, Long tagId);
    PostTag selectByPk(Long postId, Long tagId);
    List<PostTag> selectAll();
}

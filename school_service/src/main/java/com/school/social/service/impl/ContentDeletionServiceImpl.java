package com.school.social.service.impl;

import com.school.social.entity.Comment;
import com.school.social.entity.Post;
import com.school.social.mapper.CommentMapper;
import com.school.social.mapper.FavoriteMapper;
import com.school.social.mapper.LikeMapper;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.PostMediaMapper;
import com.school.social.mapper.PostTagMapper;
import com.school.social.service.ContentDeletionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ContentDeletionServiceImpl implements ContentDeletionService {
    private static final int LIKE_TARGET_POST = 0;
    private static final int LIKE_TARGET_COMMENT = 1;

    @Resource
    private PostMapper postMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private PostTagMapper postTagMapper;

    @Resource
    private PostMediaMapper postMediaMapper;

    @Override
    @Transactional
    public int deleteComment(Long commentId) {
        if (commentId == null) {
            return 0;
        }
        Comment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            return 0;
        }

        int removed = 1;
        if (existing.getParentId() == null) {
            List<Long> replyIds = commentMapper.selectIdsByParentId(commentId);
            if (replyIds != null && !replyIds.isEmpty()) {
                likeMapper.deleteByTargetIds(LIKE_TARGET_COMMENT, replyIds);
                commentMapper.deleteByParentId(commentId);
                removed += replyIds.size();
            }
        }

        likeMapper.deleteByTarget(LIKE_TARGET_COMMENT, commentId);
        commentMapper.deleteById(commentId);
        if (existing.getPostId() != null) {
            postMapper.decreaseCommentCountBy(existing.getPostId(), removed);
        }
        return removed;
    }

    @Override
    @Transactional
    public boolean deletePost(Long postId) {
        if (postId == null) {
            return false;
        }
        Post existing = postMapper.selectById(postId);
        if (existing == null) {
            return false;
        }

        List<Long> commentIds = commentMapper.selectIdsByPostId(postId);
        if (commentIds != null && !commentIds.isEmpty()) {
            likeMapper.deleteByTargetIds(LIKE_TARGET_COMMENT, commentIds);
            commentMapper.deleteByPostId(postId);
        }

        favoriteMapper.deleteByPostId(postId);
        likeMapper.deleteByTarget(LIKE_TARGET_POST, postId);
        postTagMapper.deleteByPostId(postId);
        postMediaMapper.deleteByPostId(postId);
        postMapper.deleteById(postId);
        return true;
    }
}

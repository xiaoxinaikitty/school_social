package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.interaction.CommentCreateRequest;
import com.school.social.entity.Comment;
import com.school.social.entity.Post;
import com.school.social.mapper.CommentMapper;
import com.school.social.mapper.PostMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Resource
    private CommentMapper commentMapper;

    @Resource
    private PostMapper postMapper;

    @PostMapping
    public ApiResponse<Comment> create(@RequestBody CommentCreateRequest request,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        if (request == null || request.getPostId() == null || request.getContent() == null
                || request.getContent().trim().isEmpty()) {
            return ApiResponse.fail("参数不能为空");
        }
        Post post = postMapper.selectById(request.getPostId());
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null) {
                return ApiResponse.fail("回复的评论不存在");
            }
            if (!parent.getPostId().equals(request.getPostId())) {
                return ApiResponse.fail("回复的评论不属于该内容");
            }
        }
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setStatus(0);
        comment.setLikeCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        postMapper.increaseCommentCount(request.getPostId());
        return ApiResponse.success(comment);
    }

    @GetMapping
    public ApiResponse<PageResponse<Comment>> list(@RequestParam Long postId,
                                                   @RequestParam(required = false) Long parentId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Comment> list = commentMapper.selectByPostIdPaged(postId, parentId, offset, safeSize);
        long total = commentMapper.countByPostId(postId, parentId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Comment existing = commentMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        if (!userId.equals(existing.getUserId())) {
            return ApiResponse.fail("无权限操作");
        }
        int removed = 1;
        if (existing.getParentId() == null) {
            int replyCount = commentMapper.countByParentId(id);
            if (replyCount > 0) {
                commentMapper.deleteByParentId(id);
                removed += replyCount;
            }
        }
        commentMapper.deleteById(id);
        postMapper.decreaseCommentCountBy(existing.getPostId(), removed);
        return ApiResponse.success(null);
    }
}


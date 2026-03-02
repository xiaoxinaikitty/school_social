package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.dto.post.PostCreateRequest;
import com.school.social.dto.post.PostDetailResponse;
import com.school.social.dto.post.PostMediaRequest;
import com.school.social.dto.post.PostUpdateRequest;
import com.school.social.dto.interaction.PostStatsResponse;
import com.school.social.entity.Role;
import com.school.social.entity.UserRole;
import com.school.social.entity.Post;
import com.school.social.entity.PostMedia;
import com.school.social.entity.PostTag;
import com.school.social.mapper.PostMapper;
import com.school.social.mapper.PostMediaMapper;
import com.school.social.mapper.PostTagMapper;
import com.school.social.mapper.RoleMapper;
import com.school.social.mapper.TagMapper;
import com.school.social.mapper.UserRoleMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostMediaMapper postMediaMapper;

    @Resource
    private PostTagMapper postTagMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @PostMapping
    public ApiResponse<PostDetailResponse> create(@Validated @RequestBody PostCreateRequest request,
                                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPostType(request.getPostType());
        post.setVisibility(request.getVisibility());
        post.setLocation(request.getLocation());
        post.setCollege(request.getCollege());
        boolean isDraft = request.getDraft() != null && request.getDraft();
        post.setStatus(isDraft ? 3 : 0);
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        if (!isDraft) {
            post.setPublishedAt(now);
        }
        postMapper.insert(post);

        saveTags(post.getId(), request.getTagIds());
        saveMedia(post.getId(), request.getMedia());

        return ApiResponse.success(buildDetail(post.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<PostDetailResponse> update(@PathVariable Long id,
                                                  @Validated @RequestBody PostUpdateRequest request,
                                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Post existing = postMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (userId == null || !userId.equals(existing.getUserId())) {
            return ApiResponse.fail("无权限操作");
        }
        Post update = new Post();
        update.setId(id);
        update.setTitle(request.getTitle());
        update.setContent(request.getContent());
        update.setPostType(request.getPostType());
        update.setVisibility(request.getVisibility());
        update.setLocation(request.getLocation());
        update.setCollege(request.getCollege());
        if (request.getDraft() != null) {
            boolean isDraft = request.getDraft();
            if (isDraft) {
                update.setStatus(3);
            } else {
                update.setStatus(0);
            }
            if (!isDraft && existing.getPublishedAt() == null) {
                update.setPublishedAt(LocalDateTime.now());
            }
        }
        update.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(update);

        if (request.getTagIds() != null) {
            postTagMapper.deleteByPostId(id);
            saveTags(id, request.getTagIds());
        }
        if (request.getMedia() != null) {
            postMediaMapper.deleteByPostId(id);
            saveMedia(id, request.getMedia());
        }

        return ApiResponse.success(buildDetail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Post existing = postMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.success(null);
        }
        if (userId == null || !userId.equals(existing.getUserId())) {
            return ApiResponse.fail("无权限操作");
        }
        postTagMapper.deleteByPostId(id);
        postMediaMapper.deleteByPostId(id);
        postMapper.deleteById(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDetailResponse> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Post existing = postMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 1) {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null || (!userId.equals(existing.getUserId()) && !isAdmin(userId))) {
                return ApiResponse.fail("内容未审核通过");
            }
        }
        return ApiResponse.success(buildDetail(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<Post>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Integer status) {
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        Integer queryStatus = status;
        if (queryStatus == null) {
            queryStatus = 1;
        }
        List<Post> list = postMapper.selectPaged(queryStatus, offset, safeSize);
        long total = postMapper.countByStatus(queryStatus);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/recommend")
    public ApiResponse<PageResponse<Post>> recommend(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectRecommendPaged(offset, safeSize);
        long total = postMapper.countRecommend();
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/latest")
    public ApiResponse<PageResponse<Post>> latest(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectLatestPaged(offset, safeSize);
        long total = postMapper.countLatest();
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/hot")
    public ApiResponse<PageResponse<Post>> hot(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectHotPaged(offset, safeSize);
        long total = postMapper.countHot();
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/follow")
    public ApiResponse<PageResponse<Post>> follow(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectByFolloweePaged(userId, offset, safeSize);
        long total = postMapper.countByFollowee(userId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/topic")
    public ApiResponse<PageResponse<Post>> topic(@RequestParam Long tagId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        if (tagId == null) {
            return ApiResponse.fail("tagId 不能为空");
        }
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.selectByTagPaged(tagId, offset, safeSize);
        long total = postMapper.countByTag(tagId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<Post>> search(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long tagId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        List<Post> list = postMapper.searchPaged(keyword, tagId, offset, safeSize);
        long total = postMapper.countSearch(keyword, tagId);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<Post>> related(@PathVariable Long id,
                                           @RequestParam(defaultValue = "6") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Long> tagIds = postTagMapper.selectTagIdsByPostId(id);
        if (tagIds == null || tagIds.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        List<Post> list = postMapper.selectRelatedByTagIds(tagIds, id, safeLimit);
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<PostStatsResponse> stats(@PathVariable Long id, HttpServletRequest httpRequest) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return ApiResponse.fail("内容不存在");
        }
        if (post.getStatus() == null || post.getStatus() != 1) {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null || (!userId.equals(post.getUserId()) && !isAdmin(userId))) {
                return ApiResponse.fail("内容未审核通过");
            }
        }
        PostStatsResponse resp = new PostStatsResponse();
        resp.setPostId(post.getId());
        resp.setLikeCount(post.getLikeCount());
        resp.setCommentCount(post.getCommentCount());
        resp.setFavoriteCount(post.getFavoriteCount());
        resp.setViewCount(post.getViewCount());
        return ApiResponse.success(resp);
    }

    private void saveTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> existingIds = tagMapper.selectExistingIds(tagIds);
        if (existingIds == null || existingIds.isEmpty()) {
            return;
        }
        Set<Long> idSet = new HashSet<>(existingIds);
        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            if (!idSet.contains(tagId)) {
                continue;
            }
            PostTag postTag = new PostTag();
            postTag.setPostId(postId);
            postTag.setTagId(tagId);
            postTagMapper.insert(postTag);
        }
    }

    private void saveMedia(Long postId, List<PostMediaRequest> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return;
        }
        for (PostMediaRequest media : mediaList) {
            if (media == null || media.getUrl() == null || media.getUrl().isEmpty()) {
                continue;
            }
            PostMedia entity = new PostMedia();
            entity.setPostId(postId);
            entity.setMediaType(media.getMediaType());
            entity.setUrl(media.getUrl());
            entity.setSortOrder(media.getSortOrder());
            entity.setWidth(media.getWidth());
            entity.setHeight(media.getHeight());
            entity.setDurationSec(media.getDurationSec());
            entity.setCreatedAt(LocalDateTime.now());
            postMediaMapper.insert(entity);
        }
    }

    private PostDetailResponse buildDetail(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return null;
        }
        PostDetailResponse resp = new PostDetailResponse();
        resp.setId(post.getId());
        resp.setUserId(post.getUserId());
        resp.setTitle(post.getTitle());
        resp.setContent(post.getContent());
        resp.setPostType(post.getPostType());
        resp.setStatus(post.getStatus());
        resp.setVisibility(post.getVisibility());
        resp.setLocation(post.getLocation());
        resp.setCollege(post.getCollege());
        resp.setLikeCount(post.getLikeCount());
        resp.setCommentCount(post.getCommentCount());
        resp.setFavoriteCount(post.getFavoriteCount());
        resp.setViewCount(post.getViewCount());
        resp.setCreatedAt(post.getCreatedAt());
        resp.setUpdatedAt(post.getUpdatedAt());
        resp.setPublishedAt(post.getPublishedAt());
        resp.setTagIds(postTagMapper.selectTagIdsByPostId(postId));
        resp.setMedia(postMediaMapper.selectByPostId(postId));
        return resp;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        Role role = roleMapper.selectByName("admin");
        if (role == null) {
            return false;
        }
        UserRole link = userRoleMapper.selectByPk(userId, role.getId());
        return link != null;
    }
}

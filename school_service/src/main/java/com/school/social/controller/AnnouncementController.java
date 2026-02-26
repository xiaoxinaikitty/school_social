package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.common.PageResponse;
import com.school.social.entity.Announcement;
import com.school.social.mapper.AnnouncementMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    @Resource
    private AnnouncementMapper announcementMapper;

    @GetMapping
    public ApiResponse<PageResponse<Announcement>> list(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Integer status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        List<Announcement> list = announcementMapper.selectPaged(status, offset, safeSize);
        long total = announcementMapper.countByStatus(status);
        return ApiResponse.success(new PageResponse<>(safePage, safeSize, total, list));
    }

    @GetMapping("/{id}")
    public ApiResponse<Announcement> detail(@PathVariable Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            return ApiResponse.fail("公告不存在");
        }
        return ApiResponse.success(announcement);
    }
}


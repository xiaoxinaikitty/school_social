package com.school.social.controller;

import com.school.social.common.ApiResponse;
import com.school.social.entity.Tag;
import com.school.social.mapper.TagMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    @Resource
    private TagMapper tagMapper;

    @GetMapping
    public ApiResponse<List<Tag>> list(@RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Integer status) {
        List<Tag> list = tagMapper.selectAll();
        if (type != null) {
            list = list.stream()
                    .filter(tag -> type.equals(tag.getType()))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            list = list.stream()
                    .filter(tag -> status.equals(tag.getStatus()))
                    .collect(Collectors.toList());
        }
        return ApiResponse.success(list);
    }
}


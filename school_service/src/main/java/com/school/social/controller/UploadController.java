package com.school.social.controller;

import com.school.social.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("文件不能为空");
        }
        String baseDir = System.getProperty("user.dir") + File.separator + "uploads";
        File dir = new File(baseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return ApiResponse.fail("无法创建上传目录");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = timestamp + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        File target = new File(dir, filename);
        try {
            file.transferTo(target);
        } catch (IOException ex) {
            return ApiResponse.fail("上传失败");
        }

        String url = "/uploads/" + filename;
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("name", original);
        data.put("size", file.getSize());
        return ApiResponse.success(data);
    }
}

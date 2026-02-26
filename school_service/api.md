# API 文档（已实现）

## 1. 认证模块

### 1.1 用户注册
- **URL**: `POST /api/auth/register`
- **说明**: 注册新用户（用户名必填，邮箱/手机号二选一可选）
- **请求体**（JSON）:
```json
{
  "username": "alice",
  "password": "123456",
  "email": "alice@school.edu",
  "phone": "",
  "school": "某某大学",
  "college": "计算机学院",
  "grade": "2022",
  "bio": "你好",
  "gender": 1
}
```
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice",
    "email": "alice@school.edu",
    "phone": null,
    "avatarUrl": null,
    "gender": 1,
    "school": "某某大学",
    "college": "计算机学院",
    "grade": "2022",
    "bio": "你好",
    "status": 0,
    "lastLoginAt": null,
    "createdAt": "2026-02-24T18:30:00"
  }
}
```
- **失败示例**:
```json
{
  "code": -1,
  "message": "用户名已存在",
  "data": null
}
```

### 1.2 用户登录
- **URL**: `POST /api/auth/login`
- **说明**: 登录支持用户名/邮箱/手机号三种方式
- **请求体**（JSON）:
```json
{
  "account": "alice@school.edu",
  "password": "123456",
  "loginType": "email"
}
```
- **loginType 可选值**:
  - `username`：用户名
  - `email`：邮箱
  - `phone`：手机号
  - 不传：自动识别（含 @ 视为邮箱，纯数字视为手机号，否则用户名）
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token-here",
    "user": {
      "id": 1,
      "username": "alice",
      "email": "alice@school.edu",
      "phone": null,
      "avatarUrl": null,
      "gender": 1,
      "school": "某某大学",
      "college": "计算机学院",
      "grade": "2022",
      "bio": "你好",
      "status": 0,
      "lastLoginAt": "2026-02-24T18:40:00",
      "createdAt": "2026-02-24T18:30:00"
    }
  }
}
```
- **失败示例**:
```json
{
  "code": -1,
  "message": "账号或密码错误",
  "data": null
}
```

### 1.3 退出登录
- **URL**: `POST /api/auth/logout`
- **说明**: 退出登录（当前实现为前端清理本地凭证）
- **请求体**: 无
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

## 2. 个人中心

### 2.1 获取用户资料
- **URL**: `GET /api/users/{id}`
- **说明**: 获取指定用户的个人资料
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice",
    "email": "alice@school.edu",
    "phone": "13800000000",
    "avatarUrl": null,
    "gender": 1,
    "birthday": "2003-06-12",
    "school": "某某大学",
    "college": "计算机学院",
    "grade": "2022",
    "bio": "你好",
    "status": 0,
    "lastLoginAt": "2026-02-24T18:40:00",
    "createdAt": "2026-02-24T18:30:00",
    "updatedAt": "2026-02-25T10:00:00"
  }
}
```

### 2.2 更新用户资料
- **URL**: `PUT /api/users/{id}`
- **说明**: 更新个人资料（仅修改提交的字段）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "email": "alice@school.edu",
  "phone": "13800000000",
  "avatarUrl": "https://example.com/avatar.png",
  "gender": 1,
  "birthday": "2003-06-12",
  "school": "某某大学",
  "college": "计算机学院",
  "grade": "2022",
  "bio": "你好"
}
```
- **响应**: 同“获取用户资料”

### 2.3 获取兴趣标签
- **URL**: `GET /api/users/{id}/tags`
- **说明**: 获取用户已选择的兴趣标签
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "tagId": 1, "tagName": "学习", "type": 0, "weight": 1.0 },
    { "tagId": 2, "tagName": "社团", "type": 0, "weight": 1.0 }
  ]
}
```

### 2.4 更新兴趣标签
- **URL**: `PUT /api/users/{id}/tags`
- **说明**: 覆盖式更新用户兴趣标签（传入 tagIds 列表）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "tagIds": [1, 2, 3]
}
```
- **响应**: 同“获取兴趣标签”

### 2.5 我的发布
- **URL**: `GET /api/users/{id}/posts`
- **说明**: 获取用户发布的内容列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "size": 10,
    "total": 28,
    "list": [ { "id": 1, "title": "..." } ]
  }
}
```

### 2.6 我的收藏
- **URL**: `GET /api/users/{id}/favorites`
- **说明**: 获取用户收藏的内容列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: 同“我的发布”分页结构（list 为 Post 列表）

### 2.7 我的评论
- **URL**: `GET /api/users/{id}/comments`
- **说明**: 获取用户评论记录
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: 同“我的发布”分页结构（list 为 Comment 列表）

## 3. 通用说明
- 响应结构统一：`{ code, message, data }`
- `code = 0` 表示成功；`code = -1` 表示失败
- 时间字段格式：`yyyy-MM-dd'T'HH:mm:ss`

## 4. 内容发布与管理

### 4.1 发布内容
- **URL**: `POST /api/posts`
- **说明**: 发布内容，支持标签与多媒体；`draft=true` 代表草稿
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "title": "标题",
  "content": "正文内容",
  "postType": 0,
  "visibility": 0,
  "location": "图书馆",
  "college": "计算机学院",
  "draft": false,
  "tagIds": [1, 2],
  "media": [
    { "mediaType": 0, "url": "https://example.com/a.png", "sortOrder": 1 }
  ]
}
```
- **状态约定**: `status=0` 待审，`status=1` 通过，`status=2` 驳回，`status=3` 草稿

### 4.2 编辑内容
- **URL**: `PUT /api/posts/{id}`
- **说明**: 编辑内容（可覆盖标签/媒体）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**: 同发布内容（字段可选）

### 4.3 删除内容
- **URL**: `DELETE /api/posts/{id}`
- **说明**: 删除内容
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`

### 4.4 内容详情
- **URL**: `GET /api/posts/{id}`
- **说明**: 获取内容详情（包含 tagIds 与 media）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`

### 4.5 内容列表
- **URL**: `GET /api/posts`
- **说明**: 分页获取内容列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`（默认 1），`size`（默认 10，最大 50），`status`（可选）
- **响应**: `PageResponse<Post>`

### 4.6 媒体上传
- **URL**: `POST /api/upload`
- **说明**: 上传图片/视频文件，返回可访问 URL
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**: `multipart/form-data`，字段名 `file`
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "/uploads/20260225153000_xxx.png",
    "name": "original.png",
    "size": 123456
  }
}
```

## 5. 内容浏览与发现

### 5.1 推荐流（首页）
- **URL**: `GET /api/posts/recommend`
- **说明**: 获取推荐内容列表（按置顶/精选/质量分/时间排序）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.2 关注流（仅关注用户）
- **URL**: `GET /api/posts/follow`
- **说明**: 获取关注用户的内容列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.3 最新内容
- **URL**: `GET /api/posts/latest`
- **说明**: 获取最新内容列表（按发布时间倒序）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.4 热门内容
- **URL**: `GET /api/posts/hot`
- **说明**: 获取热门内容列表（按互动热度排序）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **分页参数**: `page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.5 话题列表（标签）
- **URL**: `GET /api/tags`
- **说明**: 获取话题/标签列表，可按类型/状态过滤
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `type`（可选），`status`（可选）
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": 1, "name": "学习", "type": 0, "status": 1, "createdAt": "2026-02-24T10:00:00" }
  ]
}
```

### 5.6 话题内容列表
- **URL**: `GET /api/posts/topic`
- **说明**: 按标签获取内容列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `tagId`（必填），`page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.7 搜索（关键词与标签）
- **URL**: `GET /api/posts/search`
- **说明**: 按关键词（标题/正文）与标签搜索内容
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `keyword`（可选），`tagId`（可选），`page`（默认 1），`size`（默认 10，最大 50）
- **响应**: `PageResponse<Post>`

### 5.8 相关推荐
- **URL**: `GET /api/posts/{id}/related`
- **说明**: 获取与当前内容相同标签的相关推荐
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `limit`（默认 6，最大 20）
- **响应**: `List<Post>`

## 6. 互动与社交

### 6.1 点赞（内容/评论）
- **URL**: `POST /api/likes`
- **说明**: 对内容或评论点赞
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "targetType": 0,
  "targetId": 1001
}
```
- **targetType**: `0`=内容，`1`=评论
- **响应**:
```json
{ "code": 0, "message": "success", "data": null }
```

### 6.2 取消点赞
- **URL**: `DELETE /api/likes`
- **说明**: 取消点赞
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `targetType`，`targetId`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.3 收藏内容
- **URL**: `POST /api/favorites`
- **说明**: 收藏内容
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{ "postId": 1001 }
```
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.4 取消收藏
- **URL**: `DELETE /api/favorites/{postId}`
- **说明**: 取消收藏
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.5 发布评论/回复
- **URL**: `POST /api/comments`
- **说明**: 发布评论；传 `parentId` 表示回复
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "postId": 1001,
  "content": "这条内容很赞！",
  "parentId": null
}
```
- **响应**:
```json
{ "code": 0, "message": "success", "data": { "id": 1, "postId": 1001, "userId": 2 } }
```

### 6.6 评论列表（支持查看回复）
- **URL**: `GET /api/comments`
- **说明**: 查询评论列表，`parentId` 为空时为一级评论
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `postId`（必填），`parentId`（可选），`page`，`size`
- **响应**: `PageResponse<Comment>`

### 6.7 删除评论
- **URL**: `DELETE /api/comments/{id}`
- **说明**: 删除本人评论（含删除其回复并同步扣减计数）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.8 分享内容
- **URL**: `POST /api/shares`
- **说明**: 记录分享行为（当前仅记录行为日志）
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{ "postId": 1001, "channel": "wechat" }
```
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.9 关注用户
- **URL**: `POST /api/follows/{followeeId}`
- **说明**: 关注指定用户
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.10 取消关注
- **URL**: `DELETE /api/follows/{followeeId}`
- **说明**: 取消关注
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 6.11 关注列表
- **URL**: `GET /api/follows/following`
- **说明**: 获取当前用户关注列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`，`size`
- **响应**: `PageResponse<UserView>`

### 6.12 粉丝列表
- **URL**: `GET /api/follows/followers`
- **说明**: 获取当前用户粉丝列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`，`size`
- **响应**: `PageResponse<UserView>`

### 6.13 关注统计
- **URL**: `GET /api/follows/stats`
- **说明**: 获取当前用户关注/粉丝数量
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{ "code": 0, "message": "success", "data": { "followingCount": 3, "followerCount": 5 } }
```

### 6.14 内容互动统计
- **URL**: `GET /api/posts/{id}/stats`
- **说明**: 获取内容点赞/评论/收藏/浏览统计
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": { "postId": 1001, "likeCount": 2, "commentCount": 3, "favoriteCount": 1, "viewCount": 8 }
}
```

## 7. 消息与通知

### 7.1 通知列表
- **URL**: `GET /api/notifications`
- **说明**: 获取当前用户通知列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`（默认 1），`size`（默认 10，最大 50），`isRead`（可选，0=未读，1=已读）
- **响应**: `PageResponse<Notification>`

### 7.2 未读数量
- **URL**: `GET /api/notifications/unread-count`
- **说明**: 获取未读通知数量
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{ "code": 0, "message": "success", "data": 5 }
```

### 7.3 标记已读
- **URL**: `PUT /api/notifications/{id}/read`
- **说明**: 标记单条通知为已读
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 7.4 全部已读
- **URL**: `PUT /api/notifications/read-all`
- **说明**: 标记全部通知为已读
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**: `{ "code": 0, "message": "success", "data": null }`

### 7.5 公告列表
- **URL**: `GET /api/announcements`
- **说明**: 获取系统公告列表
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`（默认 1），`size`（默认 10，最大 50），`status`（可选）
- **响应**: `PageResponse<Announcement>`

### 7.6 公告详情
- **URL**: `GET /api/announcements/{id}`
- **说明**: 获取公告详情
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": { "id": 1, "title": "系统维护通知", "content": "...", "status": 0 }
}
```

### 7.7 通知类型说明
- **type**: `0`点赞，`1`评论，`2`回复，`3`关注，`4`系统/审核
- **refType**: `0`内容，`1`评论，`2`用户

## 8. 举报与反馈

### 8.1 提交举报
- **URL**: `POST /api/reports`
- **说明**: 举报内容/评论/用户
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **请求体**（JSON）:
```json
{
  "targetType": 0,
  "targetId": 1001,
  "reason": "不实信息",
  "detail": "内容含有虚假描述"
}
```
- **targetType**: `0`内容，`1`评论，`2`用户
- **响应**:
```json
{ "code": 0, "message": "success", "data": { "id": 1, "status": 0 } }
```

### 8.2 我的举报记录
- **URL**: `GET /api/reports`
- **说明**: 查看当前用户的举报记录与处理结果
- **鉴权**: 需要在请求头携带 `Authorization: Bearer <token>`
- **参数**: `page`（默认 1），`size`（默认 10，最大 50），`status`（可选）
- **status**: `0`待处理，`1`已处理
- **响应**: `PageResponse<Report>`


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
- **状态约定**: `status=0` 已发布，`status=1` 草稿

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


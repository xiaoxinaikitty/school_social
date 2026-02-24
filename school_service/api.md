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
```
- **失败示例**:
```json
{
  "code": -1,
  "message": "账号或密码错误",
  "data": null
}
```

## 2. 通用说明
- 响应结构统一：`{ code, message, data }`
- `code = 0` 表示成功；`code = -1` 表示失败
- 时间字段格式：`yyyy-MM-dd'T'HH:mm:ss`


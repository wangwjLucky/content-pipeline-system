# 内容生产流水线系统 — 接口测试用例

> 版本：v1.0 | 日期：2026-07-20

---

## TC-AUTH 认证模块

### TC-AUTH-001 登录成功
| 项目 | 内容 |
|------|------|
| **前置条件** | 系统已初始化，admin 用户存在且密码为 admin123 |
| **测试步骤** | POST `/api/v1/auth/login`，body: `{"username":"admin","password":"admin123"}` |
| **预期结果** | 返回 200，code=200，data 中包含 token 和 nickname |

### TC-AUTH-002 登录失败-错误密码
| 项目 | 内容 |
|------|------|
| **前置条件** | 同上 |
| **测试步骤** | POST `/api/v1/auth/login`，body: `{"username":"admin","password":"wrongpass"}` |
| **预期结果** | 返回 200，code=401，message="用户名或密码错误" |

### TC-AUTH-003 登录失败-空密码
| 项目 | 内容 |
|------|------|
| **前置条件** | 同上 |
| **测试步骤** | POST `/api/v1/auth/login`，body: `{"username":"admin","password":""}` |
| **预期结果** | 返回 400，message="密码不能为空" |

### TC-AUTH-004 登录失败-用户不存在
| 项目 | 内容 |
|------|------|
| **前置条件** | 同上 |
| **测试步骤** | POST `/api/v1/auth/login`，body: `{"username":"nonexistent","password":"somepass"}` |
| **预期结果** | 返回 200，code=401，message="用户名或密码错误" |

### TC-AUTH-005 获取当前用户-成功
| 项目 | 内容 |
|------|------|
| **前置条件** | 已获取有效 token |
| **测试步骤** | GET `/api/v1/auth/me`，header: `Authorization: Bearer <token>` |
| **预期结果** | 返回 200，data 包含 id, username, nickname, roleId |

### TC-AUTH-006 获取当前用户-无 Token
| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **测试步骤** | GET `/api/v1/auth/me`，不携带 Authorization 头 |
| **预期结果** | 返回 200，code=401，message="未认证" |

### TC-AUTH-007 获取当前用户-无效 Token
| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **测试步骤** | GET `/api/v1/auth/me`，header: `Authorization: Bearer invalid_token_here` |
| **预期结果** | 返回 200，code=401，message="未认证" |

---

## TC-TOPIC 选题管理模块

### TC-TOPIC-001 创建选题
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录获取 token |
| **测试步骤** | POST `/api/v1/topics`，body: `{"title":"Test Topic API Test","source":"MANUAL","status":"PENDING"}` |
| **预期结果** | 返回 200，data 包含 id、title、status="PENDING" |

### TC-TOPIC-002 获取选题列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/topics?page=1&size=20` |
| **预期结果** | 返回 200，data 包含 records 分页列表 |

### TC-TOPIC-003 获取单个选题
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在（id=1） |
| **测试步骤** | GET `/api/v1/topics/1` |
| **预期结果** | 返回 200，data.id=1，data.title 不为空 |

### TC-TOPIC-004 获取不存在的选题
| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **测试步骤** | GET `/api/v1/topics/99999` |
| **预期结果** | 返回 200，code=404，message="选题不存在" |

### TC-TOPIC-005 更新选题
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在（id=4） |
| **测试步骤** | PUT `/api/v1/topics/4`，body: `{"title":"Test Topic Updated","source":"MANUAL","status":"PENDING"}` |
| **预期结果** | 返回 200，data.title 已更新 |

### TC-TOPIC-006 生成任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在（id=4） |
| **测试步骤** | POST `/api/v1/topics/4/generate-task` |
| **预期结果** | 返回 200，data 包含 task 信息，status="SCRIPTING" |

### TC-TOPIC-007 删除选题
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在且无关联任务 |
| **测试步骤** | DELETE `/api/v1/topics/{id}` |
| **预期结果** | 返回 200，code=200 |

### TC-TOPIC-008 删除有关联任务的选题（异常场景）
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在且有关联任务 |
| **测试步骤** | DELETE `/api/v1/topics/4`（该选题已生成任务） |
| **预期结果** | 返回 400 或 409，提示"选题有关联任务，无法删除" |

---

## TC-TASK 任务管理模块

### TC-TASK-001 创建任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 选题已存在（topicId=1） |
| **测试步骤** | POST `/api/v1/tasks`，body: `{"topicId":1,"title":"Test Task from API"}` |
| **预期结果** | 返回 200，data.status="SCRIPTING"，progress=10 |

### TC-TASK-002 获取任务列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/tasks?page=1&size=20` |
| **预期结果** | 返回 200，data 包含 records 分页列表 |

### TC-TASK-003 获取单个任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在（id=6） |
| **测试步骤** | GET `/api/v1/tasks/6` |
| **预期结果** | 返回 200，data.id=6 |

### TC-TASK-004 获取不存在的任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **测试步骤** | GET `/api/v1/tasks/99999` |
| **预期结果** | 返回 200，code=404，message="任务不存在" |

### TC-TASK-005 取消任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在且未处于终态 |
| **测试步骤** | POST `/api/v1/tasks/{id}/cancel`，body: `{"operator":"admin","comment":"cancel test"}` |
| **预期结果** | 返回 200，任务状态变为 CANCELLED |

### TC-TASK-006 取消已取消的任务（异常场景）
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已处于 CANCELLED 状态 |
| **测试步骤** | POST `/api/v1/tasks/{id}/cancel` |
| **预期结果** | 返回 400，提示"不允许的状态转换" |

### TC-TASK-007 重试任务
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已处于 ERROR 状态 |
| **测试步骤** | POST `/api/v1/tasks/{id}/retry` |
| **预期结果** | 返回 200，任务状态变为 SCRIPTING |

### TC-TASK-008 获取任务时间线
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在（id=7） |
| **测试步骤** | GET `/api/v1/tasks/7/timeline` |
| **预期结果** | 返回 200，data 为事件列表，包含 fromStatus/toStatus |

---

## TC-SCRIPT 脚本管理模块

### TC-SCRIPT-001 获取脚本列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/scripts?page=1&size=20` |
| **预期结果** | 返回 200，data 包含 records 分页列表 |

### TC-SCRIPT-002 获取单个脚本
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本已存在（id=1） |
| **测试步骤** | GET `/api/v1/scripts/1` |
| **预期结果** | 返回 200，data.id=1 |

### TC-SCRIPT-003 获取不存在的脚本
| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **测试步骤** | GET `/api/v1/scripts/99999` |
| **预期结果** | 返回 200，code=404，message="脚本不存在" |

### TC-SCRIPT-004 脚本生成
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在（taskId=1） |
| **测试步骤** | POST `/api/v1/scripts/generate`，body: `{"taskId":1,"topicTitle":"Test Topic"}` |
| **预期结果** | 返回 200，data 包含脚本信息，status="PENDING_REVIEW" |

### TC-SCRIPT-005 编辑脚本
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本已存在（id=1） |
| **测试步骤** | PUT `/api/v1/scripts/1`，body: `{"content":"Updated content","subtitle":"Updated subtitle"}` |
| **预期结果** | 返回 200，content 和 subtitle 已更新，version 递增 |

### TC-SCRIPT-006 审核脚本-通过
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本存在且状态为 PENDING_REVIEW |
| **测试步骤** | POST `/api/v1/scripts/{id}/approve`，body: `{"reviewerId":1}` |
| **预期结果** | 返回 200，脚本状态变为 APPROVED，任务状态变为 STORYBOARD |

### TC-SCRIPT-007 审核脚本-驳回
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本存在且任务状态为 SCRIPT_REVIEW |
| **测试步骤** | POST `/api/v1/scripts/{id}/reject`，body: `{"reviewerId":1,"reason":"需要改进"}` |
| **预期结果** | 返回 200，脚本状态变为 REJECTED，任务状态变为 WAIT |

### TC-SCRIPT-008 审核脚本-无效操作
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本已存在 |
| **测试步骤** | POST `/api/v1/scripts/{id}/review`，body: `{"action":"invalid","reviewerId":1}` |
| **预期结果** | 返回 400，提示"无效操作" |

### TC-SCRIPT-009 获取脚本版本
| 项目 | 内容 |
|------|------|
| **前置条件** | 脚本已存在（id=1） |
| **测试步骤** | GET `/api/v1/scripts/1/versions` |
| **预期结果** | 返回 200，data 为版本列表 |

---

## TC-PROD 生产流程模块

### TC-PROD-001 分镜列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在（taskId=4） |
| **测试步骤** | GET `/api/v1/tasks/4/storyboard` |
| **预期结果** | 返回 200，data 为分镜列表 |

### TC-PROD-002 分镜保存
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | PUT `/api/v1/tasks/4/storyboard`，body: `[{"sequence":1,"duration":5,"sceneType":"medium","character":"host","action":"introduction","environment":"studio","camera":"front","lighting":"bright","style":"modern","aiPrompt":"A modern studio setup"}]` |
| **预期结果** | 返回 200 |

### TC-PROD-003 分镜自动拆分
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | POST `/api/v1/tasks/4/storyboard/auto-split` |
| **预期结果** | 返回 200 |

### TC-PROD-004 素材列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/materials?taskId=4` |
| **预期结果** | 返回 200，data 为素材列表 |

### TC-PROD-005 素材批量生成
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | POST `/api/v1/materials/batch-generate?taskId=4` |
| **预期结果** | 返回 200 |

### TC-PROD-006 配音生成
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | POST `/api/v1/voices/generate`，body: `{"taskId":4,"voiceType":"doubao"}` |
| **预期结果** | 返回 200 |

### TC-PROD-007 配音查询
| 项目 | 内容 |
|------|------|
| **前置条件** | 配音记录已存在 |
| **测试步骤** | GET `/api/v1/voices/4` |
| **预期结果** | 返回 200，data 包含配音信息 |

### TC-PROD-008 配音更新
| 项目 | 内容 |
|------|------|
| **前置条件** | 配音记录已存在 |
| **测试步骤** | PUT `/api/v1/voices/4`，body: `{"voiceType":"doubao","speed":1.2}` |
| **预期结果** | 返回 200，speed 已更新 |

### TC-PROD-009 剪辑编译
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | POST `/api/v1/edits/4/compile` |
| **预期结果** | 返回 200 |

### TC-PROD-010 剪辑预览
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在 |
| **测试步骤** | GET `/api/v1/edits/4/preview` |
| **预期结果** | 返回 200，data 包含 previewUrl 和 status |

---

## TC-PUB 发布管理模块

### TC-PUB-001 创建发布记录
| 项目 | 内容 |
|------|------|
| **前置条件** | 任务已存在（taskId=4） |
| **测试步骤** | POST `/api/v1/publish`，body: `{"taskId":4,"platform":"douyin","accountId":1,"title":"Test Video","tags":"test,api"}` |
| **预期结果** | 返回 200，data.status="PENDING" |

### TC-PUB-002 发布操作
| 项目 | 内容 |
|------|------|
| **前置条件** | 发布记录已存在，任务状态为 READY |
| **测试步骤** | POST `/api/v1/publish/{id}/publish` |
| **预期结果** | 返回 200，任务状态变为 PUBLISHED |

### TC-PUB-003 发布-任务未就绪（异常场景）
| 项目 | 内容 |
|------|------|
| **前置条件** | 发布记录已存在，任务状态非 READY |
| **测试步骤** | POST `/api/v1/publish/{id}/publish` |
| **预期结果** | 返回 400，提示"不允许的状态转换" |

### TC-PUB-004 定时发布
| 项目 | 内容 |
|------|------|
| **前置条件** | 发布记录已存在 |
| **测试步骤** | POST `/api/v1/publish/{id}/schedule`，body: `{"scheduledAt":"2026-07-21T10:00:00"}` |
| **预期结果** | 返回 200 |

### TC-PUB-005 取消发布
| 项目 | 内容 |
|------|------|
| **前置条件** | 发布记录已存在 |
| **测试步骤** | POST `/api/v1/publish/{id}/cancel` |
| **预期结果** | 返回 200 |

### TC-PUB-006 发布日历
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/publish/calendar?startDate=2026-07-01&endDate=2026-07-31` |
| **预期结果** | 返回 200，data 为日历数据 |

### TC-PUB-007 平台账号列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/publish/accounts` |
| **预期结果** | 返回 200，data 为账号列表 |

---

## TC-USR 用户/角色管理模块

### TC-USR-001 用户列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/users` |
| **预期结果** | 返回 200，data 包含 records |

### TC-USR-002 创建用户
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | POST `/api/v1/users`，body: `{"username":"testuser","password":"test123","nickname":"Test User","roleId":1}` |
| **预期结果** | 返回 200，data.username="testuser"，密码已加密 |

### TC-USR-003 更新用户
| 项目 | 内容 |
|------|------|
| **前置条件** | 用户已存在 |
| **测试步骤** | PUT `/api/v1/users/{id}`，body: `{"username":"testuser_updated","nickname":"Test User Updated","roleId":1,"status":"ACTIVE"}` |
| **预期结果** | 返回 200，data 已更新 |

### TC-USR-004 删除用户
| 项目 | 内容 |
|------|------|
| **前置条件** | 用户已存在 |
| **测试步骤** | DELETE `/api/v1/users/{id}` |
| **预期结果** | 返回 200 |

### TC-USR-005 角色列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/roles` |
| **预期结果** | 返回 200，data 包含 records |

### TC-USR-006 创建角色
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | POST `/api/v1/roles`，body: `{"name":"test_role","code":"TEST_ROLE","description":"Test role"}` |
| **预期结果** | 返回 200，data 包含角色信息 |

### TC-USR-007 删除角色
| 项目 | 内容 |
|------|------|
| **前置条件** | 角色已存在且无关联用户 |
| **测试步骤** | DELETE `/api/v1/roles/{id}` |
| **预期结果** | 返回 200 |

---

## TC-TMPL 模板/平台账号管理模块

### TC-TMPL-001 模板列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/templates` |
| **预期结果** | 返回 200，data 分页列表 |

### TC-TMPL-002 创建模板
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | POST `/api/v1/templates`，body: `{"name":"Test Template","type":"script","content":"Template content for {topic}","description":"Test template"}` |
| **预期结果** | 返回 200，data 包含模板信息 |

### TC-TMPL-003 更新模板
| 项目 | 内容 |
|------|------|
| **前置条件** | 模板已存在 |
| **测试步骤** | PUT `/api/v1/templates/{id}` |
| **预期结果** | 返回 200，data 已更新 |

### TC-TMPL-004 删除模板
| 项目 | 内容 |
|------|------|
| **前置条件** | 模板已存在 |
| **测试步骤** | DELETE `/api/v1/templates/{id}` |
| **预期结果** | 返回 200 |

### TC-TMPL-005 创建平台账号
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | POST `/api/v1/platform-accounts`，body: `{"platform":"douyin","accountName":"Test Account","accountId":"test123","status":"ACTIVE"}` |
| **预期结果** | 返回 200，data 包含账号信息 |

---

## TC-FILE 文件管理模块

### TC-FILE-001 文件上传
| 项目 | 内容 |
|------|------|
| **前置条件** | MinIO 已运行，bucket 已创建 |
| **测试步骤** | POST `/api/v1/files/upload?bucket=pipeline-temp&module=test&taskId=1`，multipart file |
| **预期结果** | 返回 200，data 包含 key、bucket、originalName、size |

### TC-FILE-002 文件列表
| 项目 | 内容 |
|------|------|
| **前置条件** | bucket 已存在 |
| **测试步骤** | GET `/api/v1/files?bucket=pipeline-temp&prefix=test/` |
| **预期结果** | 返回 200，data 为文件列表 |

### TC-FILE-003 文件下载
| 项目 | 内容 |
|------|------|
| **前置条件** | 文件已存在 |
| **测试步骤** | GET `/api/v1/files/download?bucket=pipeline-temp&key=test/1/{filename}` |
| **预期结果** | 返回 200，响应为文件二进制流 |

### TC-FILE-004 文件删除
| 项目 | 内容 |
|------|------|
| **前置条件** | 文件已存在 |
| **测试步骤** | DELETE `/api/v1/files?bucket=pipeline-temp&key=test/1/{filename}` |
| **预期结果** | 返回 200 |

---

## TC-AI AI 模型管理模块

### TC-AI-001 模型列表
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/ai-models` |
| **预期结果** | 返回 200，data 分页列表 |

### TC-AI-002 创建模型配置
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | POST `/api/v1/ai-models`，body: `{"provider":"openai","modelName":"gpt-4o","apiKey":"sk-test123","baseUrl":"https://api.openai.com/v1","enabled":true}` |
| **预期结果** | 返回 200，data 包含模型信息，apiKey 已加密 |

### TC-AI-003 模型测试连接
| 项目 | 内容 |
|------|------|
| **前置条件** | 模型配置已存在 |
| **测试步骤** | POST `/api/v1/ai-models/{id}/test` |
| **预期结果** | 返回 200，message="连接测试成功" |

### TC-AI-004 MQ 测试
| 项目 | 内容 |
|------|------|
| **前置条件** | RabbitMQ 已连接 |
| **测试步骤** | POST `/api/v1/ai-models/test-mq` |
| **预期结果** | 返回 200，message="MQ 消息发送成功" |

---

## TC-ANL 分析仪表盘模块

### TC-ANL-001 概览数据
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/analytics/overview` |
| **预期结果** | 返回 200，data 包含 totalTasks、completedTasks 等统计数据 |

### TC-ANL-002 日数据
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/analytics/daily?startDate=2026-07-01&endDate=2026-07-31` |
| **预期结果** | 返回 200，data 包含 trends 数组 |

### TC-ANL-003 选题分析
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/analytics/topics?limit=20` |
| **预期结果** | 返回 200，data 包含统计信息 |

### TC-ANL-004 账号分析
| 项目 | 内容 |
|------|------|
| **前置条件** | 已登录 |
| **测试步骤** | GET `/api/v1/analytics/accounts` |
| **预期结果** | 返回 200，data 包含账号统计 |

---

## TC-HC 健康检查模块

### TC-HC-001 健康检查
| 项目 | 内容 |
|------|------|
| **前置条件** | 服务已启动 |
| **测试步骤** | GET `/api/v1/health` |
| **预期结果** | 返回 200，data="OK" |
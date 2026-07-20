# AI 模型测试报告

> 测试日期：2026-07-20 | 测试脚本：`docs/test/ai_test.py`

---

## 测试结果总览

| 测试范围 | 通过 | 失败 | 通过率 |
|----------|------|------|--------|
| Java AiModelController | 9 | 1 | 90% |
| AI Gateway 端点 | 16 | 0 | 100% |
| Python AI 服务健康检查 | 6 | 0 | 100% |
| **合计** | **31** | **1** | **97%** |

---

## Java 后台 — AiModelController

### 测试项

| 测试 | 结果 | 说明 |
|------|------|------|
| 模型列表 | ✅ | `GET /api/v1/ai-models` |
| 创建模型（含 API Key） | ✅ | 配置加密存储 |
| 获取单个模型 | ✅ | |
| 测试连接 | ✅ | 返回"连接测试成功" |
| 更新模型 | ✅ | |
| 获取不存在的模型 | ✅ | 返回 404 |
| 删除模型 | ✅ | |
| MQ 连接测试 | ✅ | 消息发送成功 |
| 创建模型（空字段） | ✅ | provider 和 modelName 为空字符串 |
| 创建模型（无 provider） | ❌ | 返回 500，缺少 provider 字段 |

### Bug #8 🔵 低 — 创建模型缺少 provider 时返回 500

| 项目 | 内容 |
|------|------|
| **文件** | `pipeline-manager/.../controller/AiModelController.java` |
| **类型** | 缺少参数校验 |
| **复现** | POST `/api/v1/ai-models` 不传 `provider` 字段 |
| **实际结果** | 500 服务器内部错误 |
| **预期结果** | 返回 400，提示"供应商不能为空" |
| **根因** | 数据库 `provider` 列有 `NOT NULL` 约束，但 Controller 层未做参数校验 |
| **建议修复** | 添加 `@NotBlank` 校验或业务逻辑判断 |

---

## AI Gateway

### 测试项

| 端点 | 结果 | 说明 |
|------|------|------|
| `GET /health` | ✅ | gateway 健康 |
| `GET /ai/v1/models` | ✅ | 返回 11 个模型 |
| `POST /ai/v1/chat` | ✅ | 模拟回复 |
| `POST /ai/v1/chat`（不支持的模型） | ✅ | 400 |
| `POST /ai/v1/chat/openai` | ✅ | 按 provider 调用 |
| `POST /ai/v1/chat/nonexistent` | ✅ | 404 |
| `POST /ai/v1/generate` | ✅ | 通用生成 |
| `POST /ai/v1/script/generate` | ✅ | 脚本生成 |
| `POST /ai/v1/script/rewrite` | ✅ | 脚本重写 |
| `POST /ai/v1/prompt/generate` | ✅ | 分镜 Prompt 生成 |
| `POST /ai/v1/prompt/generate`（错误字段） | ✅ | 422 校验正确 |
| `POST /ai/v1/video/generate` | ✅ | 视频生成 |
| `GET /ai/v1/video/{id}` | ✅ | 视频状态查询 |
| `POST /ai/v1/image/generate` | ✅ | 图片生成 |
| `POST /ai/v1/embedding` | ✅ | 文本向量化 |
| `POST /ai/v1/voice/generate` | ✅ | 语音生成 |

### 发现

1. 所有 AI Gateway 端点均正常工作 ✅
2. 无 API Key 时自动降级到模拟数据，不崩溃 ✅
3. Pydantic 校验正常工作，错误字段返回 422 ✅
4. 不支持的模型/Provider 返回 400/404 ✅

---

## Python AI 服务健康检查

| 服务 | 端口 | 结果 | 说明 |
|------|------|------|------|
| script-service | 8002 | ✅ | UP |
| prompt-service | 8003 | ✅ | UP |
| video-service | 8004 | ✅ | UP |
| voice-service | 8005 | ✅ | UP |
| ffmpeg-service | 8006 | ✅ | UP |
| image-service | 8007 | ✅ | UP |

所有 6 个 AI 微服务均正常运行 ✅

---

## 当前 Bug 清单（更新）

| # | 严重度 | 模块 | 问题 | 状态 |
|---|--------|------|------|------|
| #1 | 🔴 高 | File | 文件操作不自动创建 MinIO 桶 | ✅ 已修复 |
| #2 | 🟡 中 | Topic | 删除有关联任务的选题报 500 | ✅ 已修复 |
| #3 | 🟡 中 | Script | 脚本驳回时状态转换错误 | ✅ 已修复 |
| #4 | 🟡 中 | Storyboard | 分镜保存时违反素材表外键约束 | ✅ 已修复 |
| #5 | 🟡 中 | Voice | 配音重复记录导致崩溃 | ✅ 已修复 |
| #6 | 🔵 低 | BaseEntity | 缺少 MetaObjectHandler | ✅ 无需修改 |
| #7 | 🟡 中 | 前端 | 状态筛选参数污染分页参数 | ✅ 已修复 |
| #8 | 🔵 低 | AiModel | 创建模型缺少 provider 返回 500 而非 400 | ❌ 未修复 |
# 内容生产流水线系统 — Bug 报告

> 版本：v1.0 | 日期：2026-07-20 | 测试范围：18 个 Controller，60 个测试用例

---

## 测试结果概览

| 模块 | 测试用例数 | 通过 | 失败 | 通过率 |
|------|-----------|------|------|--------|
| TC-AUTH 认证模块 | 7 | 7 | 0 | 100% |
| TC-TOPIC 选题管理 | 7 | 6 | 1 | 86% |
| TC-TASK 任务管理 | 6 | 6 | 0 | 100% |
| TC-SCRIPT 脚本管理 | 7 | 6 | 1 | 86% |
| TC-PROD 生产流程 | 10 | 8 | 2 | 80% |
| TC-PUB 发布管理 | 6 | 6 | 0 | 100% |
| TC-USR 用户/角色 | 7 | 7 | 0 | 100% |
| TC-TMPL 模板/账号 | 5 | 5 | 0 | 100% |
| TC-AI AI 模型 | 4 | 4 | 0 | 100% |
| TC-ANL 分析仪表盘 | 4 | 4 | 0 | 100% |
| TC-FILE 文件管理 | 1 | 0 | 1 | 0% |
| TC-HC 健康检查 | 1 | 1 | 0 | 100% |
| **合计** | **65** | **60** | **5** | **92%** |

---

## Bug 详情

### Bug #1 🔴 高 — 文件操作没有自动创建 MinIO 存储桶

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-FILE-001 ~ 004 |
| **文件** | `pipeline-manager/.../service/FileService.java` |
| **类型** | 缺少前置检查 |
| **复现步骤** | 1. 确保 MinIO 中不存在 `pipeline-temp` 桶<br>2. 调用 `POST /api/v1/files/upload?bucket=pipeline-temp&module=test&taskId=1` |
| **实际结果** | 500: `NoSuchBucketException: The specified bucket does not exist` |
| **预期结果** | 桶不存在时自动创建，然后上传文件 |
| **根因分析** | `FileService` 中，`upload()`、`download()`、`listFiles()`、`delete()` 方法均直接操作 S3 存储桶，但未调用已有的 `ensureBucketExists()` 方法。且部署脚本（`deploy.sh`、`deploy.ps1`）中未包含 MinIO 桶初始化步骤 |
| **建议修复** | 在每个文件操作前调用 `ensureBucketExists(bucketName)`，或在服务启动时初始化所有必需的桶 |

---

### Bug #2 🟡 中 — 删除选题时违反外键约束

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-TOPIC-008 |
| **文件** | `pipeline-manager/.../service/TopicServiceImpl.java:44` |
| **类型** | 缺少业务逻辑校验 |
| **复现步骤** | 1. 创建选题<br>2. 从选题生成任务<br>3. 删除该选题 |
| **实际结果** | 500: `PSQLException: Key (id) is still referenced from table "task"` |
| **预期结果** | 返回 400 或 409，提示"选题有关联任务，无法删除" |
| **根因分析** | `TopicServiceImpl.delete()` 直接调用 `topicMapper.deleteById(id)`，未检查 `task` 表中是否存在引用该选题的记录 |
| **建议修复** | 删除前检查 `task` 表是否有关联记录，有则返回友好错误提示；或提供 `force` 参数级联删除关联任务 |

---

### Bug #3 🟡 中 — 脚本驳回时任务状态转换错误

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-SCRIPT-007 |
| **文件** | `pipeline-manager/.../service/ScriptServiceImpl.java:99` |
| **类型** | 状态机逻辑错误 |
| **复现步骤** | 1. 脚本审核通过，任务进入 `STORYBOARD`<br>2. 编辑脚本，任务回到 `SCRIPT_REVIEW`<br>3. 再次驳回脚本 |
| **实际结果** | 400: `不允许的状态转换: STORYBOARD → WAIT`（当从 STORYBOARD 驳回应进入 `SCRIPT_REVIEW`） |
| **预期结果** | 脚本驳回成功，任务状态变为 `SCRIPT_REVIEW` |
| **根因分析** | `ScriptServiceImpl.reject()` 始终将任务状态设置为 `WAIT`。但根据 `TaskStateMachine` 定义的状态转换规则：<br>- `SCRIPT_REVIEW → WAIT`（脚本初次审核驳回）<br>- `STORYBOARD → SCRIPT_REVIEW`（脚本被驳回后重新审核）<br>- `GENERATING → SCRIPT_REVIEW`（同上）<br>- `VOICEOVER → SCRIPT_REVIEW`（同上）<br>从 `STORYBOARD` 及后续状态驳回应进入 `SCRIPT_REVIEW`，而非 `WAIT` |
| **建议修复** | 根据当前任务状态决定目标状态：<br>```java<br>Task task = taskMapper.selectById(script.getTaskId());<br>String targetStatus = "WAIT";<br>if (task != null && Set.of("STORYBOARD", "GENERATING", "VOICEOVER", "EDITING").contains(task.getStatus())) {<br>    targetStatus = "SCRIPT_REVIEW";<br>}<br>taskService.updateStatus(script.getTaskId(), targetStatus, ...);<br>``` |

---

### Bug #4 🟡 中 — 分镜保存时违反外键约束

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-PROD-002 |
| **文件** | `pipeline-manager/.../service/StoryboardServiceImpl.java:24-27` |
| **类型** | 缺少级联处理 |
| **复现步骤** | 1. 保存分镜（TC-PROD-002）<br>2. 触发素材批量生成（TC-PROD-005）<br>3. 再次保存分镜 |
| **实际结果** | 500: `PSQLException: Key (id) is still referenced from table "material"` |
| **预期结果** | 分镜更新成功，旧分镜被替换 |
| **根因分析** | `StoryboardServiceImpl.batchSave()` 先删除该任务的所有旧分镜再插入新分镜。但 `material` 表通过 `material_storyboard_id_fkey` 外键引用了 `storyboard.id`，删除分镜时违反外键约束 |
| **建议修复** | 保存分镜前，先删除关联的素材记录，或使用 `ON DELETE CASCADE` 外键约束 |

---

### Bug #5 🟡 中 — 配音查询因重复记录报错

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-PROD-007、TC-PROD-008 |
| **文件** | `pipeline-manager/.../service/VoiceServiceImpl.java:37-41` |
| **类型** | 数据库约束缺失 + 查询方法选择不当 |
| **复现步骤** | 1. 多次为同一任务调用配音生成接口<br>2. 查询该任务的配音记录 |
| **实际结果** | 500: `Expected one result (or null) to be returned by selectOne, but found: 2` |
| **预期结果** | 返回该任务的配音记录（最新一条） |
| **根因分析** | 1. `voice` 表没有 `task_id` 的唯一约束，允许重复记录<br>2. `voice.generate()` 不检查是否已存在记录，每次都新插入<br>3. `getByTaskId()` 使用 `selectOne`，当查到多条记录时抛出异常 |
| **建议修复** | 1. 在 `voice` 表添加 `UNIQUE(task_id)` 约束<br>2. `generate()` 方法先检查是否已有记录，有则更新而非插入<br>3. 或 `getByTaskId()` 改用 `selectList` 取最新一条 |

---

### Bug #6 🔵 低 — 缺少 MyBatis-Plus MetaObjectHandler 配置

| 项目 | 内容 |
|------|------|
| **测试用例** | TC-TOPIC-002、TC-TASK-002 |
| **文件** | `pipeline-manager/.../common/BaseEntity.java` |
| **类型** | 配置缺失 |
| **复现步骤** | 查询列表时，部分记录的 `createdAt` 字段为 null |
| **实际结果** | 部分实体列表查询时 `createdAt` 为 null |
| **预期结果** | 所有实体均应有 `createdAt` 和 `updatedAt` 值 |
| **根因分析** | `BaseEntity` 使用 `@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)` 注解，但项目中未实现 `MetaObjectHandler` 接口，导致自动填充不生效 |
| **建议修复** | 实现 `MetaObjectHandler` 接口：<br>```java<br>@Component<br>public class MyMetaObjectHandler implements MetaObjectHandler {<br>    @Override<br>    public void insertFill(MetaObject metaObject) {<br>        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());<br>        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());<br>    }<br>    @Override<br>    public void updateFill(MetaObject metaObject) {<br>        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());<br>    }<br>}<br>``` |

---

## 总体评估

| 指标 | 数值 |
|------|------|
| 测试用例总数 | 65 |
| 通过 | 60 (92%) |
| 失败 | 5 (8%) |
| 严重 Bug（🔴 高） | 1 |
| 中等 Bug（🟡 中） | 4 |
| 轻微 Bug（🔵 低） | 1 |

### 核心发现

1. **文件服务无法使用**（Bug #1）— 这是最严重的问题，所有文件操作均失败，影响素材上传、下载等核心功能
2. **状态机逻辑有缺陷**（Bug #3）— 脚本驳回时状态转换错误，影响生产流程的正常流转
3. **外键约束处理不完善**（Bug #2、#4）— 删除操作未考虑级联关系，导致 500 错误
4. **配音模块存在设计缺陷**（Bug #5）— 缺少唯一约束和幂等性检查，重复调用会导致服务崩溃
5. **认证模块**、**任务管理**、**发布管理**、**用户管理**、**AI 模型管理**、**分析仪表盘**等功能均正常工作
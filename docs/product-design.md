# 内容生产流水线系统 — 产品文档

> 版本：v2.0  
> 日期：2026-07-21  
> 状态：产品设计评审后重构（含架构演进建议）

---

## 1. 产品定位

### 1.1 产品概述

内容生产流水线系统（Content Pipeline System）是一套面向 AI 技术类抖音账号的**半自动化内容生产平台**。它支持 **4 种内容产出方式**（视频/纯文案/纯图片/图文），将内容从选题到发布的各阶段标准化、模块化、可编排，通过 Java 平台 + Python AI 服务的混合架构，实现"选题采集 → 脚本生成 → 分镜拆解 → 素材生成 → 配音 → 剪辑 → 发布"的全流程管理。

不同产出方式走不同的生产流程：

| 产出方式 | 产出内容 | 生产流程 |
|----------|----------|----------|
| `video`（视频） | 完整视频 | 选题→脚本→分镜→素材→配音→剪辑→发布 |
| `text`（纯文案） | 文案/脚本文字 | 选题→脚本→审核→发布 |
| `image`（纯图片） | 信息图/海报 | 选题→脚本→分镜→图片生成→审核→发布 |
| `image_text`（图文） | 图片+文案 | 选题→脚本→分镜→图片生成→审核→发布 |

### 1.2 核心价值

| 维度 | 说明 |
|------|------|
| **提效** | 一条内容从 2-3 小时缩短到 30 分钟以内（含人工审核），纯文案仅需 5-10 分钟 |
| **标准化** | 不同产出方式各有标准化流程，新人培训成本大幅降低 |
| **可扩展** | 支持多账号、多平台、多模型，矩阵运营 |
| **可追溯** | 每条内容的全流程状态可见，问题环节一目了然 |

### 1.3 目标用户

| 角色 | 描述 | 核心需求 |
|------|------|----------|
| **内容运营** | 负责选题、审核成片 | 热点选题推荐、快速审核、一键发布 |
| **视频编辑** | 负责分镜、剪辑（视频/图片/图文类型） | 分镜模板、素材管理、批量剪辑 |
| **管理员** | 负责系统配置、账号管理 | 模型配置、平台接入、权限管理、数据统计 |
| **AI 系统** | 后台自动化服务 | 脚本生成、视频生成、配音、字幕等自动化任务 |

---

## 2. 功能清单

### 2.1 功能全景图

```
内容生产流水线系统
│
├── 内容产出方式
│   ├── 视频（video）— 完整流程：脚本→分镜→素材→配音→剪辑→发布
│   ├── 纯文案（text）— 简化流程：脚本→审核→发布
│   ├── 纯图片（image）— 图片流程：脚本→分镜→图片→审核→发布
│   └── 图文（image_text）— 图片+文案：脚本→分镜→图片→审核→发布
│
├── 第一阶段：选题管理
│   ├── 热点采集（GitHub Trending / Hacker News / Reddit / X / AI 新闻站）
│   ├── 选题筛选（热度评分 + 关键词匹配 + 四个维度评估）
│   ├── 选题库（待做 / 进行中 / 已完成 / 已废弃）
│   └── 手动录入
│
├── 第二阶段：脚本管理
│   ├── AI 脚本生成（基于 Prompt 模板）
│   ├── 脚本编辑（标题 / 文案 / 字幕）
│   ├── 脚本审核（批准/驳回，驳回后支持重新生成或手动编辑）
│   ├── 纯文案迭代循环：AI 生成 → 人工编辑 → 重新生成 → 满意后提交审核
│   ├── Prompt 模板管理
│   └── 脚本版本管理
│
├── 第三阶段：分镜管理
│   ├── 自动拆解脚本为镜头
│   ├── 分镜编辑（镜头编号 / 时长 / 景别 / 画面描述）
│   ├── 分镜模板库
│   └── AI Prompt 生成（可灵 / Veo 等文生视频 Prompt）
│
├── 第四阶段：素材生成
│   ├── AI 视频生成任务管理
│   ├── 多模型支持（可灵 / Veo / 即梦 / Sora）
│   ├── 图片生成（可选）
│   └── 素材库（MinIO 存储）
│
├── 第五阶段：配音管理
│   ├── AI 配音（豆包 / ElevenLabs）
│   ├── 语音克隆
│   ├── 语速调整（默认 1.05x）
│   └── 多音色管理
│
├── 第六阶段：剪辑合成
│   ├── FFmpeg 自动合成
│   ├── 自动字幕（关键词高亮：放大 / 黄色 / 描边）
│   ├── BGM 管理（科技 / 轻音乐分类）
│   ├── 音效管理（Whoosh / Click / Pop）
│   └── 封面生成（文字：3-8 个字）
│
├── 第七阶段：发布管理
│   ├── 多平台发布（抖音主账号 / 矩阵号）
│   ├── 定时发布（10:00 / 18:00 / 21:00）
│   ├── 标题与封面管理
│   ├── 标签管理（#AI #程序员 #ChatGPT 等）
│   ├── 评论区管理（预设评论：Prompt / 源码 / 教程）
│   ├── 发布日历
│   └── 纯文案发布：无需视频素材，审核通过后直接进入发布流程
│
├── 数据分析
│   ├── 视频表现分析（播放 / 点赞 / 评论 / 转发）
│   ├── 选题效果分析
│   ├── 账号成长趋势
│   └── 运营日报 / 周报
│
├── 系统管理
│   ├── 用户与权限（RBAC）
│   ├── 模型配置（AI Gateway 路由）
│   ├── 平台账号管理
│   ├── 素材清理
│   └── 操作日志
│
└── AI 服务（Python 后端）
    ├── 脚本生成服务 (script-service)
    ├── Prompt 生成服务 (prompt-service)
    ├── 视频生成服务 (video-service)
    ├── 图片生成服务 (image-service)
    ├── 配音服务 (voice-service)
    └── 剪辑合成服务 (ffmpeg-service)
```

### 2.2 功能优先级（MVP vs V2）

#### MVP（第一阶段可上线）

| 模块 | 功能 | 优先级 |
|------|------|--------|
| 选题管理 | 手动录入选题 | P0 |
| 选题管理 | 选题库 CRUD | P0 |
| 脚本管理 | AI 脚本生成（接入 Claude/GPT） | P0 |
| 脚本管理 | 脚本编辑与审核 | P0 |
| 脚本管理 | **纯文案产出：脚本编辑→重新生成→审核→发布** | P0 |
| 分镜管理 | 手动拆解分镜 | P0 |
| 分镜管理 | AI Prompt 生成 | P0 |
| 素材生成 | AI 视频生成（至少一个模型） | P0 |
| 素材生成 | **图片生成（纯图片/图文产出）** | P0 |
| 配音管理 | AI 配音 | P0 |
| 剪辑合成 | FFmpeg 自动合成 | P0 |
| 剪辑合成 | 自动字幕 | P0 |
| 发布管理 | 手动发布 | P0 |
| 发布管理 | **纯文案直接发布** | P0 |
| 系统管理 | 用户与权限 | P0 |

#### V2

| 模块 | 功能 | 优先级 |
|------|------|--------|
| 选题管理 | 热点自动采集 | P1 |
| 选题管理 | 热度评分自动推荐 | P1 |
| 分镜管理 | 自动拆解分镜 | P1 |
| 分镜管理 | 分镜模板库 | P2 |
| 素材生成 | 多模型并行生成 | P1 |
| 剪辑合成 | 封面自动生成 | P1 |
| 发布管理 | 定时发布 | P1 |
| 发布管理 | 多平台矩阵发布 | P1 |
| 数据分析 | 全部功能 | P1 |
| 剪辑合成 | BGM / 音效自动匹配 | P2 |
| 配音管理 | 语音克隆 | P2 |

---

## 3. 业务流程

### 3.1 核心流程 — 一条内容的生命周期

系统支持 4 种内容产出方式，不同方式走不同的生产流程：

```
视频（video）:
  [选题] → [脚本] → [分镜] → [素材] → [配音] → [剪辑] → [发布]

纯文案（text）:
  [选题] → [脚本] → ┌→ [审核] → [发布]
                     │      │
                     └── 驳回 ──→ [编辑/重新生成] ──→ 满意后重新提交审核

纯图片（image）:
  [选题] → [脚本] → [审核] → [分镜] → [图片生成] → [审核] → [发布]

图文（image_text）:
  [选题] → [脚本] → [审核] → [分镜] → [图片生成] → [审核] → [发布]
```

### 3.2 详细状态流转

不同产出方式走不同的状态路径：

```
视频（video）:
  WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING
  → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED

纯文案（text）:
  WAIT → SCRIPTING → SCRIPT_REVIEW → READY → PUBLISHED

纯图片（image）:
  WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING
  → REVIEW → READY → PUBLISHED

图文（image_text）:
  WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING
  → REVIEW → READY → PUBLISHED
```

所有类型共享以下通用处理：

```
                  ┌─────────────┐
                  │   WAIT      │  ← 创建任务，等待处理
                  └──────┬──────┘
                         ▼
                  ┌─────────────┐
                  │  SCRIPTING  │  ← AI 生成脚本（所有类型）
                  └──────┬──────┘
                         ▼
                  ┌─────────────┐
                  │ SCRIPT_REVIEW│  ← 人工审核脚本
                  └──────┬──────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │STORYBOARD│   │  READY   │   │  WAIT   │
   │ 分镜生成  │   │ 纯文案通过│   │ 脚本驳回│
   └────┬─────┘   └──────────┘   └──────────┘
        ▼
   ┌──────────┐
   │GENERATING│  ← 生成视频/图片素材
   └────┬─────┘
        │
   ┌────┴────┬──────────┐
   ▼         ▼          ▼
┌──────┐ ┌──────┐ ┌──────────┐
│REVIEW│ │VOICE │ │SCRIPT    │
│图/文 │ │OVER  │ │_REVIEW   │
│终审  │ │配音  │ │脚本驳回  │
└──────┘ └──┬───┘ └──────────┘
            ▼
       ┌───────┐
       │EDITING│  ← FFmpeg 剪辑合成
       └───┬───┘
           ▼
       ┌───────┐
       │REVIEW │  ← 人工终审
       └───┬───┘
           ▼
       ┌───────┐
       │ READY │  ← 待发布
       └───┬───┘
           ▼
       ┌──────────┐
       │PUBLISHED │  ← 已发布
       └──────────┘

          ERROR ← 任意步骤失败（可重试）
```

### 3.3 人工介入点

不同产出方式的人工介入点不同。所有类型的脚本阶段均支持**迭代循环**：AI 生成 → 人工编辑 → 驳回重审/重新生成 → 满意后批准。

| 产出方式 | 脚本迭代 | 分镜编辑 | 素材审核 | 终审 |
|----------|----------|----------|----------|------|
| 视频 | ✅ AI 生成→编辑→重生成→审核 | ✅ 5 分钟 | ✅ 5 分钟 | ✅ 30 秒 |
| **纯文案** | **✅ AI 生成→编辑→重生成→审核（核心环节）** | — | — | — |
| 纯图片 | ✅ AI 生成→编辑→重生成→审核 | ✅ 5 分钟 | ✅ 5 分钟 | ✅ 30 秒 |
| 图文 | ✅ AI 生成→编辑→重生成→审核 | ✅ 5 分钟 | ✅ 5 分钟 | ✅ 30 秒 |

> **纯文案的特殊性**：纯文案产出的核心价值在于脚本本身。用户可在脚本审核页面反复编辑、重新生成，直到对文案内容满意后再提交审核。审核通过后直接进入待发布状态，无需分镜/素材/配音/剪辑等环节。

---

## 4. 用户界面（页面规划）

### 4.1 页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录 | `/login` | 用户认证登录 |
| 工作台 | `/dashboard` | 今日任务概览、待审核数量、近 7 天发布统计 |
| 选题管理 | `/topics` | 选题列表、热度评分、批量操作 |
| 选题编辑 | `/topics/new` | 新增选题 |
| 选题编辑 | `/topics/:id/edit` | 编辑选题 |
| 任务列表 | `/tasks` | 任务列表、状态筛选 |
| 任务详情 | `/tasks/:id` | 完整任务流水线视图、当前状态、重试按钮、内容类型标识 |
| 脚本管理 | `/scripts` | 脚本列表、状态筛选、内容类型筛选 |
| 脚本审核 | `/scripts/:id` | 编辑脚本、重新生成、提交审核。纯文案类型在此完成全部人工干预（迭代编辑→重生成→审核→发布） |
| 分镜管理 | `/storyboards/:taskId` | 分镜列表、镜头编辑、AI 自动拆分 |
| 配音管理 | `/voice` | 配音生成、参数设置 |
| 剪辑管理 | `/edits` | 剪辑合成任务管理 |
| 素材库 | `/materials` | 已生成的图片/视频素材 |
| 发布管理 | `/publish` | 待发布列表、定时发布设置 |
| 发布日历 | `/publish/calendar` | 日历视图 |
| 数据分析 | `/analytics` | 播放数据、趋势图表 |
| 模板管理 | `/templates` | Prompt 模板、分镜模板 |
| 模型配置 | `/settings/models` | AI 模型参数、API Key |
| 平台账号 | `/settings/platform-accounts` | 多平台账号管理 |
| 用户管理 | `/settings/users` | 系统用户管理 |
| 角色管理 | `/settings/roles` | 角色权限管理 |

### 4.2 工作台原型说明

工作台是用户每天打开的第一个页面，核心信息：

```
┌─────────────────────────────────────────────────┐
│  📋 今日概览                                      │
│  待审核脚本: 3  待审核成片: 5  今日待发布: 2      │
│  📋 内容类型分布: 视频 8  文案 3  图片 2  图文 1  │
├─────────────────────────────────────────────────┤
│  📊 近 7 天发布趋势 (折线图)                      │
│  ┌───────────────────────────────────────────┐  │
│  │              ▁▃▆▇▅▇▆                       │  │
│  └───────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  ⏳ 进行中的任务                                  │
│  ┌─────────────────────────────────────────────┐│
│  │ 视频 #023  │ ████████░░ 80% │ 配音中       ││
│  │ 文案 #018  │ ██████░░░░ 60% │ 脚本编辑中   ││
│  │ 图片 #015  │ ███████░░░ 70% │ 素材生成中   ││
│  │ 视频 #021  │ ███████░░░ 70% │ 素材生成中   ││
│  └─────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

---

## 5. 产品路线图

### Phase 1 — MVP（4-6 周）

**目标：跑通核心流程，人工为主 AI 为辅**

- 选题手动录入
- 脚本 AI 生成 + 人工审核
- 手动分镜 + AI Prompt 生成
- 接入一个视频生成模型（首选可灵）
- AI 配音（豆包）
- FFmpeg 剪辑合成 + 自动字幕
- 人工审核发布
- 用户权限管理

### Phase 2 — 自动化 + AI 任务中心（2-3 周）

**目标：减少人工介入，提升生产效率**

- 热点自动采集（GitHub Trending / Hacker News / RSS）
- 自动拆解分镜
- 多模型并行生成（可灵 + Veo + 即梦）
- 定时发布
- 素材库管理
- **AI 任务中心**：统一 AI 任务管理、状态追踪、失败重试
- **Prompt 中心**：模板版本管理、变量注入、A/B 测试

### Phase 3 — 矩阵运营 + Pipeline 引擎（2-3 周）

**目标：支持多账号多平台矩阵运营，Pipeline 可配置**

- 多平台账号管理
- 矩阵发布策略
- 评论区管理
- 数据分析
- 运营日报
- **Pipeline 定义引擎**：流水线可配置化，新增产出方式无需改代码
- **事件总线**：服务间事件驱动通信，替代硬编码链式调用
- **全链路版本化**：Topic → Script → Storyboard → Video 版本追溯

### Phase 4 — 智能化 + 工作流（持续迭代）

**目标：数据驱动选题与优化，可视化编排**

- 热点智能推荐（基于历史数据）
- 视频效果预测
- A/B 测试封面/标题
- 自动优化 Prompt 模板
- 语音克隆
- **可视化工作流**：拖拽式 Pipeline 编排
- **素材生命周期管理**：自动清理临时素材，去重与压缩

---

## 7. 数据模型

### 7.1 核心实体

当前系统已有以下核心数据实体：

| 实体 | 核心字段 | 说明 |
|------|----------|------|
| **Task** | id, topicId, title, scriptId, contentType, status, progress, errorMessage, version, createdBy | 任务 — 整个流水线的核心载体 |
| **Topic** | id, title, source, sourceUrl, hotScore, isAuto, status, createdBy | 选题 |
| **Script** | id, topicId, taskId, title, content, subtitle, version, status, promptTemplateId | 脚本（含版本管理） |
| **Storyboard** | id, taskId, sequence, duration, sceneType, character, action, environment, camera, lighting, style, aiPrompt | 分镜 |
| **Material** | id, taskId, storyboardId, type(video/image/audio), model, url, prompt, status | 素材 |
| **Voice** | id, taskId, voiceType, voiceUrl, speed, duration, status | 配音 |
| **PublishLog** | id, taskId, platform, accountId, title, coverUrl, tags, status, scheduledAt, publishedAt | 发布日志 |
| **TaskEvent** | id, taskId, fromStatus, toStatus, operator, comment | 任务事件（状态机时间线） |
| **AiModelConfig** | id, modelName, provider, apiKeyEncrypted, modelType, weight, enabled | AI 模型配置 |
| **PlatformAccount** | id, platform, accountName, cookiesEncrypted, status | 平台账号 |
| **PromptTemplate** | id, name, type, content, variables, enabled | Prompt 模板 |

### 7.2 Task 实体设计

当前 Task 字段：

```java
Task {
    id            // 主键
    topicId       // 关联选题
    title         // 标题
    scriptId      // 关联脚本
    contentType   // 产出方式: video/text/image/image_text
    status        // 状态: WAIT → SCRIPTING → ... → PUBLISHED
    progress      // 进度 0-100
    errorMessage  // 错误信息
    version       // 乐观锁版本号
    createdBy     // 创建人
}
```

**未来可扩展字段**：`owner`（当前处理人）、`priority`（优先级）、`retryCount`（重试次数）、`failReason`（失败原因归类）、`expireTime`（过期时间）。

### 7.3 状态机设计

当前状态机有 12 个状态，按内容类型走不同路径：

| 状态 | 含义 | 归属阶段 |
|------|------|----------|
| WAIT | 等待中 | 初始 |
| SCRIPTING | 脚本生成中 | 脚本 |
| SCRIPT_REVIEW | 脚本审核中 | 脚本 |
| STORYBOARD | 分镜生成中 | 分镜 |
| GENERATING | 素材生成中 | 素材 |
| VOICEOVER | 配音生成中 | 配音 |
| EDITING | 剪辑合成中 | 剪辑 |
| REVIEW | 成片审核中 | 终审 |
| READY | 待发布 | 发布 |
| PUBLISHED | 已发布 | 终态 |
| CANCELLED | 已取消 | 终态 |
| ERROR | 处理失败 | 异常 |

**未来演进建议**：将状态简化为 `INIT → RUNNING → REVIEW → READY → DONE`，用独立的 Stage 记录当前处理阶段（SCRIPT / STORYBOARD / GENERATING / VOICE / EDIT / PUBLISH），避免状态数量无限增长。

---

## 8. 系统架构演进

### 8.1 当前架构

```
Web (Vue 3 :80/:3000)
    │
    ▼
Spring Boot API (:8080)
    │
    ├── RabbitMQ ──→ Python AI 服务 (7个微服务)
    │                     │
    │                     ▼
    │               AI Gateway (:8001)
    │                     │
    │              ┌──────┼──────┐
    │              ▼      ▼      ▼
    │          Claude  GPT   DeepSeek  可灵  Veo  豆包
    │
    ├── PostgreSQL (:5432)
    ├── Redis (:6379)
    └── MinIO (:9000)
```

### 8.2 目标架构（Pipeline Engine）

```
Web (Vue 3)
    │
    ▼
Pipeline API (Spring Boot)
    │
    ├── Pipeline Engine          ← 核心：定义→实例→节点→任务
    │       │
    │       ├── Pipeline Definition   (流水线定义：video/text/image pipeline)
    │       ├── Pipeline Instance     (流水线实例：每次生产的运行实例)
    │       ├── Pipeline Node         (流水线节点：Topic→Script→Storyboard→...)
    │       └── Task                  (任务：承载具体业务数据)
    │
    ├── Task Center              ← 任务调度与编排
    │       │
    │       ├── AI Task            (抽象 AI 任务：submit/poll/cancel/retry)
    │       ├── Script Task        (脚本生成)
    │       ├── Image Task         (图片生成)
    │       ├── Video Task         (视频生成)
    │       └── Voice Task         (配音生成)
    │
    ├── Event Bus                ← 事件驱动
    │       │
    │       ├── ScriptGenerated   → 触发 Storyboard
    │       ├── StoryboardReady   → 触发 Generate
    │       ├── MaterialReady     → 触发 Voice/Edit
    │       └── EditComplete      → 触发 Review
    │
    ├── AI Gateway (:8001)
    │       │
    │       ├── Model Router (按类型+权重路由)
    │       ├── Provider (Claude / GPT / DeepSeek / 可灵 / Veo / 豆包)
    │       └── Rate Limiter / Fallback / Retry
    │
    ├── Material Center (MinIO)
    │       ├── 素材元数据管理
    │       ├── 生命周期（temp → 清理 / final → 永久）
    │       └── 去重与压缩
    │
    ├── Prompt Center
    │       ├── 模板版本管理
    │       ├── 变量注入引擎
    │       ├── A/B 测试
    │       └── 历史记录
    │
    └── Publish Service
            ├── 多平台适配器
            ├── 定时发布
            └── 发布策略

PostgreSQL / Redis / MinIO
```

### 8.3 Pipeline 定义（数据库存储）

Pipeline 定义存储在数据库中，而非 YAML 文件，支持后台可视化管理：

```sql
-- 流水线定义表
CREATE TABLE pipeline_definition (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL UNIQUE, -- video/text/image/image_text
    description     VARCHAR(500),
    version         INT DEFAULT 1,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 流水线节点表
CREATE TABLE pipeline_node (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL REFERENCES pipeline_definition(id),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    handler         VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL,
    required_review BOOLEAN DEFAULT FALSE,
    support_loop    BOOLEAN DEFAULT FALSE,
    parallel        BOOLEAN DEFAULT FALSE,
    retryable       BOOLEAN DEFAULT TRUE,
    timeout_seconds INT DEFAULT 300,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 流水线节点关系表
CREATE TABLE pipeline_node_relation (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL REFERENCES pipeline_definition(id),
    from_node_id    BIGINT NOT NULL REFERENCES pipeline_node(id),
    to_node_id      BIGINT NOT NULL REFERENCES pipeline_node(id),
    condition_expr  VARCHAR(500),
    sort_order      INT NOT NULL
);
```

**优势**：4 种产出方式的流水线存储在数据库中，后台可新增/编辑/删除 Pipeline，无需改代码。未来做拖拽式工作流时，直接操作这几张表即可。

### 8.4 Task → Run → NodeRun 执行模型

Task 与执行分离，一个 Task 可多次执行（Run）：

```
Task（任务定义）
  ├── Run #1（第一次执行）
  │     ├── NodeRun: SCRIPT      → 完成
  │     ├── NodeRun: STORYBOARD  → 完成
  │     ├── NodeRun: GENERATE    → 失败 → 重试 → 完成
  │     └── NodeRun: PUBLISH     → 完成
  │
  ├── Run #2（重新生成，复用 Run#1 的部分结果）
  │     ├── NodeRun: GENERATE    → 完成（重新生成）
  │     └── NodeRun: PUBLISH     → 完成
  │
  └── Run #3（再次修改后发布）
        └── ...
```

核心表设计：

```sql
CREATE TABLE task_run (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id),
    run_number      INT NOT NULL,
    status          VARCHAR(20) DEFAULT 'RUNNING',
    triggered_by    VARCHAR(50),           -- AUTO/MANUAL/RETRY
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_node_run (
    id              BIGSERIAL PRIMARY KEY,
    run_id          BIGINT NOT NULL REFERENCES task_run(id),
    node_code       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING',
    handler         VARCHAR(200),
    retry_count     INT DEFAULT 0,
    max_retries     INT DEFAULT 3,
    input_snapshot  TEXT,
    output_snapshot TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8.5 AI Task ProviderLog

每次 AI 调用的完整日志，用于问题排查和成本分析：

```sql
CREATE TABLE ai_task_log (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT,
    run_id          BIGINT,
    node_run_id     BIGINT,
    provider        VARCHAR(50) NOT NULL,    -- Claude/GPT/可灵/...
    model           VARCHAR(100),
    prompt          TEXT,
    response        TEXT,
    prompt_tokens   INT,
    completion_tokens INT,
    latency_ms      INT,
    cost            DECIMAL(10,6),
    retry_count     INT DEFAULT 0,
    status          VARCHAR(20),             -- SUCCESS/FAILED
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8.6 Material → Asset 资源模型

素材分为 Asset（统一资源）→ Material（具体产物）→ Render（衍生格式）：

```
Asset（原始资源）
  ├── Material: image.png → Render: thumbnail.jpg / cover.webp
  ├── Material: video.mp4 → Render: hd.mp4 / sd.mp4 / cover.jpg
  └── Material: voice.mp3 → Render: none
```

```sql
CREATE TABLE asset (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT,
    asset_type      VARCHAR(20) NOT NULL,    -- image/video/audio
    md5             VARCHAR(64),             -- 文件指纹（去重）
    size_bytes      BIGINT,
    width           INT,
    height          INT,
    duration_sec    INT,
    bucket          VARCHAR(100),
    path            VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    source          VARCHAR(50),             -- AI_GENERATED/UPLOAD/COMPRESSED
    version         INT DEFAULT 1,
    expire_at       TIMESTAMP,               -- 临时素材自动清理
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8.7 版本 DAG（Version Graph）

全链路版本追溯，形成有向无环图：

```
Topic V1
  ├── Script V1 ──→ Storyboard V1 ──→ Video V1
  └── Script V2 ──→ Storyboard V2 ──→ Video V2  ← 修改脚本后重新生成
                          └── Video V3（仅重生成视频）
```

```sql
CREATE TABLE version_graph (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(50) NOT NULL,    -- SCRIPT/STORYBOARD/VIDEO
    entity_id       BIGINT NOT NULL,
    version         INT NOT NULL,
    parent_version_id BIGINT,                -- 父版本（形成 DAG）
    snapshot        TEXT,                    -- 版本快照
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8.8 调度引擎（XXL-JOB）

系统真正的驱动力不是 Controller，而是调度引擎：

```
┌───────────────────────────────────────────────┐
│              Scheduler (XXL-JOB)               │
├───────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────────────┐   │
│  │ Task Scanner  │  │   Retry Queue        │   │
│  │ 扫描待处理任务 │  │   失败重试队列        │   │
│  └──────┬───────┘  └──────┬───────────────┘   │
│         ▼                  ▼                   │
│  ┌─────────────────────────────────────────┐  │
│  │           Dispatcher                     │  │
│  │  PriorityQ / RoundRobin / FailQ / DLQ   │  │
│  └─────────────────────────────────────────┘  │
│         ▼                  ▼                   │
│  ┌──────────────┐  ┌──────────────┐           │
│  │  Worker Pool  │  │  Monitor     │           │
│  │  执行 AI 任务  │  │  超时/健康检查 │           │
│  └──────────────┘  └──────────────┘           │
└───────────────────────────────────────────────┘
```

核心调度任务：

| 调度任务 | 说明 | 频率 |
|----------|------|------|
| pendingTaskScanner | 扫描 WAIT 状态任务，推入 Dispatcher | 每 10 秒 |
| retryQueueProcessor | 处理重试队列中的失败任务 | 每 30 秒 |
| timeoutMonitor | 检测超时未完成的任务 | 每 1 分钟 |
| tempMaterialCleaner | 清理过期临时素材 | 每天凌晨 3 点 |
| publishScheduler | 检查定时发布任务 | 每 30 秒 |

### 8.9 权限模型（增强 RBAC）

```
Role（角色）
  └── Permission（权限）
        ├── Pipeline 级别: video:create, text:edit, image:delete, ...
        ├── Node 级别:    script:review, storyboard:edit, publish:execute, ...
        └── System 级别:  user:manage, model:config, analytics:view, ...
```

### 8.10 ER 图

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│  topic   │1──N│   task   │1──N│  task_run     │
└──────────┘     └────┬─────┘     └──────┬───────┘
                      │                  │
                      ▼                  ▼
               ┌──────────┐     ┌──────────────┐
               │  script  │     │ task_node_run│
               └────┬─────┘     └──────────────┘
                    ▼
               ┌────────────┐    ┌──────────────┐
               │ storyboard │1──N│  material    │
               └────────────┘    └──────┬───────┘
                                        ▼
                                   ┌──────────┐
                                   │  asset   │
                                   └──────────┘

┌──────────┐   ┌──────────────┐   ┌──────────────┐
│  voice   │   │ publish_log  │   │ platform_act │
└──────────┘   └──────┬───────┘   └──────────────┘
                      │
┌──────────────┐  ┌──┴───────────┐  ┌──────────────────┐
│ ai_model_cfg │  │ ai_task_log  │  │ prompt_template  │
└──────────────┘  └──────────────┘  └──────────────────┘

┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│ pipeline_def │──│ pipeline_node│  │  version_graph    │
└──────────────┘  └──────────────┘  └──────────────────┘
```

### 8.11 服务拆分（远期）

建议将 Java 后台拆分为 6 个独立服务：

| 服务 | 职责 | 端口 |
|------|------|------|
| pipeline-service | Pipeline 定义 + 编排引擎 | 8101 |
| task-service | Task/Run/NodeRun 管理 + 调度 | 8102 |
| material-service | Asset/Material 管理 + MinIO | 8103 |
| publish-service | 发布 + 平台账号 + 定时 | 8104 |
| gateway-service | AI Gateway + Provider 路由 | 8105 |
| analytics-service | 数据统计 + 报表 | 8106 |

Python 保持 6 个服务不变，中间件：RabbitMQ（消息）/ Redis（锁+缓存+限流）/ XXL-JOB（调度）/ PostgreSQL（数据）/ MinIO（存储）。

---

## 9. 未来规划模块

### 9.1 AI 任务中心（建议）

统一管理所有 AI 任务的状态和生命周期：

| 功能 | 说明 |
|------|------|
| 任务列表 | 所有 AI 任务的统一视图（脚本/图片/视频/配音） |
| 状态追踪 | 每个 AI 任务的当前状态、耗时、进度 |
| Token 用量 | 统计每次 AI 调用的 Token 消耗和费用 |
| Provider 日志 | 请求/响应/延迟/费用/重试记录 |
| 失败管理 | 失败原因归类、一键重试、批量重试 |
| 取消机制 | 支持取消正在执行的 AI 任务 |

### 9.2 Prompt 中心（建议）

统一的 Prompt 管理与实验平台：

| 功能 | 说明 |
|------|------|
| 版本管理 | 每个 Prompt 模板的修改历史，可回溯 |
| 变量注入 | 结构化变量（title/topic/style/...）自动注入 |
| Provider 适配 | 同一 Prompt 自动适配不同模型的格式 |
| A/B 测试 | 同一场景下对比不同 Prompt 的效果 |
| 历史记录 | 每次 AI 调用使用的 Prompt 完整快照 |

### 9.3 工作流管理（远期）

可视化 Pipeline 编排，支持拖拽式配置：

```
当前（数据库配置）：             未来（可视化编排）：
  pipeline_definition 表      视频: [Topic]→[Script]→[Storyboard]→...
  pipeline_node 表            文案: [Topic]→[Script]→[Publish]
  pipeline_node_relation 表    └── 可拖拽增删节点
```

---

## 10. 技术架构建议

### 10.1 分层架构

```
┌─────────────────────────────────────┐
│          Web (Vue 3)                │
├─────────────────────────────────────┤
│       Pipeline API (Spring Boot)    │
├─────────────────────────────────────┤
│          Pipeline Engine            │
│  ┌──────────┬──────────┬──────────┐ │
│  │Task      │Workflow  │Event     │ │
│  │Center    │Engine    │Bus       │ │
│  └──────────┴──────────┴──────────┘ │
├─────────────────────────────────────┤
│     Scheduler (XXL-JOB)            │
│  ┌──────┬──────┬──────┬──────────┐ │
│  │Task  │Retry │Pri-  │Dead      │ │
│  │Scan  │Queue │Queue │Letter    │ │
│  └──────┴──────┴──────┴──────────┘ │
├─────────────────────────────────────┤
│          AI Gateway                 │
│  ┌──────┬──────┬──────┬──────────┐ │
│  │Claude│  GPT │Deep  │ 可灵/Veo│ │
│  │      │      │Seek  │ /豆包    │ │
│  └──────┴──────┴──────┴──────────┘ │
├─────────────────────────────────────┤
│     Material Center (MinIO)         │
├─────────────────────────────────────┤
│     FFmpeg Service / Publish Svc    │
└─────────────────────────────────────┘
```

### 10.2 关键设计原则

1. **Pipeline 驱动**：4 种产出方式各有独立 Pipeline 定义，逻辑集中管理
2. **事件驱动**：服务间通过事件总线通信，而非硬编码的 Controller 链式调用
3. **AI 任务抽象**：所有 AI 平台实现统一接口，业务层不感知具体模型
4. **版本 DAG**：Topic → Script → Storyboard → Video 全链路版本追溯
5. **素材生命周期**：Asset 统一管理，临时素材自动清理，成片永久保存
6. **调度驱动**：XXL-JOB 统一调度，替代 Controller 直连调用
7. **权限精细化**：Pipeline 级别 + Node 级别权限控制

### 10.3 与当前代码的关系

以上架构演进建议基于当前代码基础逐步演进，当前系统已实现的核心能力：

| 已实现 | 待演进 |
|--------|--------|
| 4 种内容类型支持 | Pipeline 定义引擎（DB 存储） |
| 状态机 12 状态 | 简化为 Stage + 有限状态 |
| 6 个 AI 服务 | AI Task 统一抽象 + ProviderLog |
| AI Gateway 路由 | 完备的 Provider 注册表 |
| MinIO 素材存储 | Asset 元数据管理 + 生命周期 |
| 任务事件记录 | 事件总线 + 调度引擎 |
| 脚本版本管理 | 全链路版本 DAG |
| RBAC 权限 | Pipeline/Node 级别细粒度权限 |
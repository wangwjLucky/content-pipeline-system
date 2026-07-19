# 内容生产流水线系统 — 文档索引

> 版本：v1.1 | 日期：2026-07-19

## 文档列表

| 文档 | 说明 |
|------|------|
| [架构总览](architecture.md) | 系统架构、部署拓扑、技术选型、扩展性设计 |
| [服务说明](services-overview.md) | 每个服务的配置、功能、调用链路、状态机详解 |
| [产品设计](product-design.md) | 产品定位、功能清单、业务流程、用户界面规划 |
| [技术设计](technical-design.md) | 技术选型明细、API 设计、数据模型、安全设计、MQ 设计 |
| [配置体系](configuration.md) | 配置注入链路、Python/Java 配置、环境变量清单、配置 FAQ |
| [运维部署手册](operations.md) | **部署、启动、镜像管理、日志、排查、备份 — 非技术人员可操作** |
| [启动指南（快速参考）](startup-guide.md) | 快速命令速查（完整内容见 operations.md） |
| [Docker 操作指南](docker-guide.md) | Docker 镜像构建、运行、日志、卷映射等操作命令 |
| [Docker 命令参考](docker-commands.md) | Docker CLI 命令完整手册

## 项目结构

```
content-pipeline-system/
├── ai-services/              # Python AI 服务
│   ├── common/               #   共享模块（config/callback/logging/minio/rabbit）
│   ├── gateway/              #   AI Gateway（统一 AI 路由入口）
│   ├── script-service/       #   脚本生成服务
│   ├── prompt-service/       #   分镜生成服务
│   ├── video-service/        #   视频生成服务
│   ├── voice-service/        #   配音生成服务
│   ├── image-service/        #   图片生成服务
│   └── ffmpeg-service/       #   剪辑合成服务
├── pipeline-manager/         # Java Spring Boot 后台
│   └── src/main/java/com/pipeline/admin/
│       ├── common/           #   公共工具（加密、自动填充、结果封装）
│       ├── config/           #   配置类（安全、MyBatis-Plus、定时任务、JWT）
│       ├── controller/       #   REST 控制器
│       ├── entity/           #   数据库实体
│       ├── mapper/           #   MyBatis-Plus Mapper
│       └── service/          #   业务逻辑层
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── api/              #   API 请求封装
│       ├── router/           #   路由配置
│       ├── stores/           #   Pinia 状态管理
│       └── views/            #   页面组件
├── docker-compose.yml        # Docker Compose 编排
├── init.sql                  # PostgreSQL 初始化脚本
└── .env                      # 环境变量
```
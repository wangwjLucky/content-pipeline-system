# 内容生产流水线系统 — 文档索引

> 版本：v1.0 | 日期：2026-07-17

## 文档列表

| 文档 | 说明 |
|------|------|
| [架构总览](architecture.md) | 系统架构、部署拓扑、技术选型、扩展性设计 |
| [服务说明](services-overview.md) | 每个服务的配置、功能、调用链路详解 |
| [Docker 操作指南](docker-guide.md) | Docker 镜像构建、运行、日志、卷映射等操作命令 |
| [Docker 命令参考](docker-commands.md) | Docker CLI 命令完整手册（15 大类，涵盖容器/镜像/Compose/网络/卷/安全性等） |
| [启动指南](startup-guide.md) | 开发环境和生产环境的启动步骤 |
| [技术设计](technical-design.md) | 技术选型明细、设计原则 |

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
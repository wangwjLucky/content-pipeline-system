# 内容生产流水线系统 — 总体实施路线图

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 构建完整的 AI 内容生产流水线系统，支撑从选题到发布的全流程半自动化生产

**架构:** Java Spring Boot 做平台后台，Python FastAPI 做 AI 服务层，Vue 3 做前端管理界面

**技术栈:** Java 17 + Spring Boot 3 + PostgreSQL 16 + Redis 7 + RabbitMQ + MinIO + Python 3.12 + FastAPI + Vue 3 + Ant Design Vue

---

## 阶段划分

```
Phase 1 — 项目骨架 + 基础设施 + 核心后端（当前）
Phase 2 — Python AI 服务层（MVP）
Phase 3 — Vue 3 前端管理界面
Phase 4 — 集成测试 + 完整流水线联调
Phase 5 — 自动化采集 + 数据分析 + 高级功能
```

---

# Phase 1: 项目骨架 + 基础设施 + 核心后端

**目标:** 搭建可运行的项目骨架，完成基础设施容器化，实现核心数据模型和 REST API

**产出:** `docker compose up` 后整套基础设施启动，Spring Boot 启动并暴露 API

---
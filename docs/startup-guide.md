# 服务启动指南（快速参考）

> ⚠️ **本指南已合并到 [运维部署手册](operations.md)**，完整内容请查看 `operations.md`。
>
> 版本：v2.0 | 日期：2026-07-19

---

## 快速命令

### Docker 部署（完整步骤见 [运维部署手册 → 第 4 节](operations.md#4-服务部署与启动)）

```bash
# 一键启动（开发环境）
cp .env.example .env
# 编辑 .env 文件
docker compose up -d

# 分步启动
docker compose up -d postgres redis minio rabbitmq
docker compose build pipeline-admin && docker compose up -d pipeline-admin
docker compose build ai-gateway && docker compose up -d ai-gateway
docker compose build script-service prompt-service && docker compose up -d script-service prompt-service
docker compose build video-service voice-service && docker compose up -d video-service voice-service
docker compose build ffmpeg-service image-service && docker compose up -d ffmpeg-service image-service
docker compose build frontend && docker compose up -d frontend
```

### 服务验证

```bash
# 健康检查
curl http://localhost:8080/api/v1/health     # Java 后台
curl http://localhost:8001/health             # AI Gateway
docker compose ps -a                          # 所有服务状态
```

### 常用运维命令

| 操作 | 命令 | 详见 |
|------|------|------|
| 查看日志 | `docker compose logs -f script-service` | [第 7 节](operations.md#7-日志查看与管理) |
| 重启服务 | `docker compose restart ai-gateway` | [第 5 节](operations.md#5-服务停止与重启) |
| 重建镜像 | `docker compose build --no-cache` | [第 3 节](operations.md#3-docker-镜像全生命周期管理) |
| 停止服务 | `docker compose down` | [第 5 节](operations.md#5-服务停止与重启) |
| 数据库备份 | `docker compose exec -T postgres pg_dump -U pipeline pipeline \| gzip > backup.sql.gz` | [第 10 节](operations.md#10-数据库管理) |
| 问题排查 | — | [第 12 节](operations.md#12-问题排查) |

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [运维部署手册](operations.md) | **完整部署、运维、排查指南（推荐）** |
| [架构总览](architecture.md) | 系统架构、技术选型 |
| [服务说明](services-overview.md) | 各服务功能、调用链路、状态机 |
| [配置体系](configuration.md) | 配置注入链路、环境变量清单 |
| [技术设计](technical-design.md) | 技术选型明细、API 设计、数据模型 |
| [产品设计](product-design.md) | 产品定位、功能清单、业务流程 |
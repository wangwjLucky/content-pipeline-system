# Docker 命令完整参考手册

> 涵盖 Docker Engine、Docker Compose、Dockerfile、镜像管理、容器管理、数据卷、网络、构建、监控、清理等全方位命令。
> 适用版本：Docker Engine >= 24.x，Docker Compose >= 2.x

---

## 目录

1. [基础命令](#1-基础命令)
2. [容器生命周期](#2-容器生命周期)
3. [镜像管理](#3-镜像管理)
4. [Docker Compose](#4-docker-compose)
5. [数据卷 (Volume)](#5-数据卷-volume)
6. [网络管理](#6-网络管理)
7. [Dockerfile 指令](#7-dockerfile-指令)
8. [构建命令 (Build)](#8-构建命令-build)
9. [日志与监控](#9-日志与监控)
10. [资源清理](#10-资源清理)
11. [系统管理](#11-系统管理)
12. [安全与认证](#12-安全与认证)
13. [多架构与 Buildx](#13-多架构与-buildx)
14. [故障排查](#14-故障排查)
15. [实用技巧与脚本](#15-实用技巧与脚本)

---

## 1. 基础命令

```bash
# 版本信息
docker version                  # 显示 Docker 版本信息（Client + Server）
docker info                     # 显示系统信息（容器数、镜像数、驱动等）

# 帮助
docker --help                   # Docker 帮助
docker <command> --help         # 子命令帮助（如 docker run --help）
docker compose --help           # Docker Compose 帮助
```

---

## 2. 容器生命周期

### 2.1 创建与运行

```bash
# 创建但不启动
docker create [选项] <镜像> [命令]
docker create --name my-nginx nginx:alpine

# 创建并启动（最常用）
docker run [选项] <镜像> [命令]

# 常用 run 选项
docker run -d                            # 后台运行（detached）
docker run -it                           # 交互式终端
docker run --rm                          # 退出后自动删除容器
docker run --name <名称>                  # 指定容器名
docker run -p 8080:80                     # 端口映射 主机:容器
docker run -v /host/path:/container/path # 挂载卷
docker run -e ENV_VAR=value              # 环境变量
docker run --env-file .env               # 从文件导入环境变量
docker run --restart always              # 重启策略（always/on-failure/no）
docker run --network <网络名>             # 指定网络
docker run --link <容器名>:<别名>          # 容器连接（已废弃，建议用网络）
docker run -w /app                       # 工作目录
docker run --user 1000:1000              # 指定用户运行
docker run --cpus 2                      # CPU 限制
docker run --memory 512m                 # 内存限制
docker run --memory-swap 1g              # 内存+Swap 限制
docker run --read-only                   # 只读文件系统

# 示例
docker run -d --name my-redis -p 6379:6379 redis:7-alpine
docker run -it --rm node:20-alpine /bin/sh
docker run -d --name postgres -v pgdata:/var/lib/postgresql/data -e POSTGRES_PASSWORD=secret postgres:16
```

### 2.2 启动与停止

```bash
docker start <容器>              # 启动已停止的容器
docker stop <容器>               # 优雅停止（发送 SIGTERM）
docker stop -t 30 <容器>         # 指定超时时间（秒）
docker kill <容器>               # 强制停止（发送 SIGKILL）
docker restart <容器>            # 重启
docker pause <容器>              # 暂停容器（冻结）
docker unpause <容器>            # 恢复暂停的容器
docker wait <容器>               # 阻塞直到容器退出
```

### 2.3 查看容器

```bash
docker ps                        # 查看运行中的容器
docker ps -a                     # 查看所有容器（含已停止）
docker ps -q                     # 只显示 ID（可用于管道）
docker ps -aq                    # 所有容器的 ID
docker ps --filter status=exited # 按状态过滤
docker ps --filter name=my-nginx # 按名称过滤
docker ps --format "{{.ID}} {{.Names}}"  # 自定义格式输出
docker ps -s                     # 显示大小
docker ps -l                     # 显示最新创建的容器
docker ps -n 5                   # 显示最近 5 个容器
```

### 2.4 进入容器

```bash
# 在容器中执行命令
docker exec <容器> <命令>
docker exec my-nginx ls /etc/nginx

# 交互式进入
docker exec -it <容器> /bin/bash    # 如果容器有 bash
docker exec -it <容器> /bin/sh      # Alpine 镜像用 sh
docker exec -it <容器> powershell   # Windows 容器

# 以 root 用户进入
docker exec -u root -it <容器> /bin/sh

# 设置工作目录
docker exec -w /app -it <容器> /bin/sh
```

### 2.5 删除容器

```bash
docker rm <容器>                  # 删除已停止的容器
docker rm -f <容器>               # 强制删除（运行中也能删）
docker rm -v <容器>               # 同时删除关联的匿名卷
docker rm $(docker ps -aq)       # 删除所有容器
docker rm $(docker ps -q -f status=exited)  # 删除所有已退出的容器
docker container prune            # 删除所有已停止的容器
docker container prune --filter "until=24h"  # 删除 24 小时前停止的
```

### 2.6 容器信息

```bash
docker inspect <容器>             # 查看容器详细信息（JSON）
docker inspect -f '{{.State.Status}}' <容器>  # 格式化输出特定字段
docker top <容器>                 # 查看容器内进程
docker stats                      # 查看所有容器的资源使用（实时）
docker stats <容器1> <容器2>       # 查看指定容器
docker stats --no-stream          # 单次查看，不持续监控
docker port <容器>                # 查看端口映射
docker diff <容器>                # 查看容器文件系统变更
docker logs <容器>                # 查看日志（详见日志章节）
```

---

## 3. 镜像管理

### 3.1 拉取与推送

```bash
# 拉取镜像
docker pull <镜像>                # 拉取 latest 标签
docker pull nginx:alpine          # 拉取指定标签
docker pull registry.example.com/myapp:v1.0  # 从私有仓库拉取
docker pull -a nginx              # 拉取所有标签

# 推送镜像
docker push <镜像>                # 推送到仓库
docker push myregistry.com/myapp:latest
```

### 3.2 查看与搜索

```bash
docker images                     # 列出所有镜像
docker images -a                  # 列出所有（含中间层）
docker images -q                  # 只显示 ID
docker images --filter dangling=true  # 列出悬空镜像
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"  # 自定义格式
docker image ls                   # 同 docker images

docker search <关键词>             # 搜索 Docker Hub
docker search --limit 10 nginx    # 搜索并限制结果数
docker search --filter stars=100 nginx  # 搜索星级过滤
```

### 3.3 删除镜像

```bash
docker rmi <镜像>                 # 删除镜像
docker rmi -f <镜像>              # 强制删除
docker rmi $(docker images -q)   # 删除所有镜像
docker image prune                # 删除悬空镜像（dangling）
docker image prune -a             # 删除所有未使用的镜像
docker image prune --filter "until=24h"  # 删除 24 小时前的未使用镜像
```

### 3.4 镜像标签与导出

```bash
# 打标签
docker tag <源镜像> <目标镜像>
docker tag nginx:latest my-nginx:v1.0
docker tag nginx:latest registry.example.com/nginx:v1.0

# 保存与加载（文件传输，无压缩）
docker save -o nginx.tar nginx:alpine    # 导出镜像为 tar
docker load -i nginx.tar                 # 从 tar 导入镜像

# 导出与导入（压缩格式，推荐）
docker image save nginx:alpine | gzip > nginx.tar.gz
gunzip -c nginx.tar.gz | docker image load

# 导出容器为镜像
docker export -o my-container.tar <容器>    # 导出容器快照
docker import my-container.tar my-image:v1  # 从快照导入为镜像

# 查看镜像历史
docker history <镜像>
docker history --no-trunc nginx:alpine
```

### 3.5 镜像构建

```bash
docker build -t <名称>:<标签> <路径>      # 构建镜像
docker build -t my-app:v1.0 .             # 当前目录 Dockerfile
docker build -t my-app:v1.0 -f Dockerfile.prod .  # 指定 Dockerfile
docker build --no-cache -t my-app:v1.0 .  # 不使用缓存构建
docker build --build-arg VERSION=1.0 .    # 传递构建参数
docker build --target builder .           # 构建到指定阶段（多阶段构建）

# BuildKit 特性（Docker 18.09+）
DOCKER_BUILDKIT=1 docker build -t my-app:v1.0 .  # 启用 BuildKit
docker build --ssh default .              # 传递 SSH 密钥
docker build --secret id=mysecret,src=./secret.txt .  # 传递密钥
docker build --cache-from registry.example.com/cache:latest .  # 远程缓存
```

---

## 4. Docker Compose

### 4.1 基础命令

```bash
# 必须在包含 docker-compose.yml 的目录下执行
docker compose up                 # 创建并启动所有服务（前台）
docker compose up -d              # 后台启动
docker compose up -d <服务>       # 启动特定服务
docker compose up --build         # 启动前重新构建
docker compose up --force-recreate # 强制重新创建容器
docker compose up --no-deps       # 不启动依赖服务
docker compose up --no-start      # 只创建不启动

docker compose down               # 停止并删除所有容器、网络
docker compose down -v            # 同时删除数据卷
docker compose down --rmi all     # 同时删除镜像
docker compose down --remove-orphans  # 删除未在 compose 中定义的容器
```

### 4.2 管理与查看

```bash
docker compose ps                 # 查看所有服务状态
docker compose ps -a              # 查看所有（含已停止）
docker compose ls                 # 列出所有运行中的 Compose 项目
docker compose ls -a              # 列出所有（含已停止的）

docker compose start              # 启动已存在的服务
docker compose stop               # 停止服务（不删除）
docker compose restart            # 重启所有服务
docker compose restart <服务>     # 重启特定服务
docker compose kill               # 强制停止
docker compose pause              # 暂停所有服务
docker compose unpause            # 恢复暂停
```

### 4.3 日志与调试

```bash
docker compose logs               # 查看所有服务日志
docker compose logs -f            # 实时追踪日志
docker compose logs --tail=100 -f # 最近 100 行并追踪
docker compose logs <服务>        # 特定服务日志
docker compose logs -f <服务>     # 特定服务日志实时追踪
docker compose logs --since 5m    # 最近 5 分钟
docker compose logs --no-color    # 无颜色输出（适合重定向）

docker compose exec <服务> <命令> # 在服务中执行命令
docker compose exec web /bin/sh
docker compose exec -T db psql -U postgres -c "SELECT 1"  # 非 TTY 模式

docker compose run <服务> <命令>  # 运行一次性命令
docker compose run --rm web npm test

docker compose top                # 查看各服务进程
docker compose events             # 实时事件流
```

### 4.4 镜像与构建

```bash
docker compose build              # 构建所有服务镜像
docker compose build <服务>       # 构建特定服务
docker compose build --no-cache   # 无缓存构建
docker compose build --parallel   # 并行构建
docker compose build --push       # 构建并推送到仓库

docker compose pull               # 拉取所有服务镜像
docker compose pull <服务>        # 拉取特定服务镜像
docker compose push               # 推送所有服务镜像
```

### 4.5 配置与验证

```bash
docker compose config             # 验证并查看 Compose 配置
docker compose config --services  # 列出所有服务名
docker compose config --volumes   # 列出所有数据卷
docker compose config --hash      # 显示服务配置哈希
docker compose config --output compose-resolved.yml  # 输出解析后的配置

docker compose version            # 查看 Compose 版本
```

### 4.6 扩缩容与迁移

```bash
docker compose up -d --scale web=5   # 将 web 服务扩展到 5 个实例
docker compose up -d --scale worker=3 --scale web=2

docker compose cp <服务>:/path/to/file ./local/path/  # 从容器复制文件
docker compose cp ./local/path <服务>:/path/to/file   # 复制到容器
```

---

## 5. 数据卷 (Volume)

### 5.1 卷管理

```bash
# 创建与查看
docker volume create <卷名>           # 创建数据卷
docker volume create --driver local --label env=prod my-volume  # 带标签
docker volume ls                      # 列出所有卷
docker volume ls -q                   # 只显示卷名
docker volume ls --filter dangling=true  # 列出未被使用的卷
docker volume ls --format "table {{.Name}}\t{{.Driver}}\t{{.Mountpoint}}"
docker volume inspect <卷名>          # 查看卷详情

# 删除
docker volume rm <卷名>               # 删除卷
docker volume rm $(docker volume ls -q)  # 删除所有卷
docker volume prune                   # 删除所有未使用的卷
docker volume prune --filter "label!=keep"  # 保留有特定标签的卷
docker volume prune -f                # 强制删除，不提示
```

### 5.2 绑定挂载

```bash
# 运行容器时挂载
docker run -v /host/path:/container/path <镜像>   # 绝对路径
docker run -v $(pwd):/app <镜像>                   # 当前目录
docker run -v ./data:/data <镜像>                   # 相对路径
docker run -v /data:/data:ro <镜像>                # 只读挂载
docker run -v /data:/data:z <镜像>                 # SELinux 标签
docker run -v /data:/data:Z <镜像>                 # 私有 SELinux 标签

# 使用 --mount 语法（推荐，更明确）
docker run --mount type=bind,source=/host/path,target=/container/path <镜像>
docker run --mount type=bind,source=$(pwd),target=/app,readonly <镜像>

# 使用数据卷
docker run --mount type=volume,source=my-volume,target=/data <镜像>
docker run --mount type=tmpfs,target=/tmp <镜像>   # 临时文件系统（内存）
```

### 5.3 卷备份与恢复

```bash
# 备份卷到主机
docker run --rm -v my-volume:/source -v $(pwd)/backup:/backup alpine \
  tar czf /backup/my-volume-$(date +%Y%m%d).tar.gz -C /source .

# 从备份恢复卷
docker run --rm -v my-volume:/target -v $(pwd)/backup:/backup alpine \
  tar xzf /backup/my-volume-backup.tar.gz -C /target

# 卷间复制
docker run --rm -v source-volume:/from -v target-volume:/to alpine \
  sh -c "cp -av /from/. /to/"

# 备份 PostgreSQL
docker exec -t postgres pg_dump -U <user> <db> > backup.sql
docker exec -t postgres pg_dumpall -U postgres > all-dbs-backup.sql
```

---

## 6. 网络管理

### 6.1 网络类型

| 网络驱动 | 说明 | 适用场景 |
|---------|------|---------|
| `bridge` | 默认网络，容器间通过 IP 通信 | 单机部署（默认） |
| `host` | 容器直接使用主机网络栈 | 高性能场景、网络监控 |
| `overlay` | 跨主机的容器网络 | Swarm 集群 |
| `macvlan` | 容器使用独立 MAC 地址 | 遗留应用、网络策略 |
| `none` | 无网络 | 需要完全隔离的容器 |
| `ipvlan` | 容器共享 MAC 但不同 IP | 大规模部署 |

### 6.2 网络操作

```bash
# 创建网络
docker network create <网络名>              # 创建 bridge 网络
docker network create -d overlay my-overlay # 创建 overlay 网络
docker network create --subnet 172.20.0.0/16 --ip-range 172.20.5.0/24 my-net
docker network create --driver bridge --attachable my-net
docker network create --label env=prod --internal my-net  # 内部网络（无外网）

# 查看网络
docker network ls                        # 列出网络
docker network ls -q                     # 只显示 ID
docker network inspect <网络名>           # 查看网络详情
docker network inspect bridge | grep -A 10 "Containers"  # 查看连接到网络的容器

# 连接/断开容器
docker network connect <网络名> <容器>    # 将容器连接到网络
docker network connect --ip 172.20.5.10 my-net my-container  # 指定 IP
docker network connect --alias db my-net my-container  # 指定网络别名
docker network disconnect <网络名> <容器>  # 断开容器连接
docker network disconnect -f <网络名> <容器>  # 强制断开

# 删除网络
docker network rm <网络名>                # 删除网络
docker network prune                      # 删除所有未使用的网络
docker network prune --filter "until=24h"
```

### 6.3 网络调试

```bash
# 测试容器间连通性
docker exec <容器1> ping <容器2>
docker exec <容器1> curl http://<容器2>:8080/health

# 查看网络命名空间
docker inspect <容器> --format '{{.NetworkSettings.IPAddress}}'  # 查看容器 IP
docker inspect <容器> --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'

# DNS 解析测试
docker run --rm --network my-net alpine nslookup <服务名>

# 抓包
docker run --rm --net=host nicolaka/netshoot tcpdump -i eth0
```

---

## 7. Dockerfile 指令

### 7.1 指令大全

```dockerfile
# ===== 基础 =====
FROM <镜像>                      # 基础镜像
FROM node:20-alpine AS builder   # 多阶段构建命名
FROM scratch                     # 空镜像（构建纯二进制镜像）

# ===== 元信息 =====
LABEL maintainer="dev@example.com"  # 标签
LABEL version="1.0" description="My app"
LABEL org.opencontainers.image.source="https://github.com/..."

# ===== 文件操作 =====
COPY . /app                      # 复制文件（保留源文件属性）
COPY --chown=node:node . /app    # 设置所有者
COPY --chmod=755 entry.sh /entry.sh  # 设置权限
COPY --from=builder /build/app /app  # 从构建阶段复制
ADD app.tar.gz /app              # 自动解压 tar/zip
ADD https://example.com/file /file  # 支持 URL 下载

# ===== 运行命令 =====
RUN apt-get update && apt-get install -y curl  # 安装依赖
RUN pip install -r requirements.txt
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt  # BuildKit 缓存
RUN --mount=type=secret,id=mysecret cat /run/secrets/mysecret  # BuildKit 密钥

# ===== 环境配置 =====
ENV NODE_ENV=production          # 环境变量
ENV PORT=8080 \
    HOST=0.0.0.0
ARG VERSION=1.0                  # 构建参数（仅构建时有效）
ARG DEBIAN_FRONTEND=noninteractive

# ===== 工作目录 =====
WORKDIR /app                     # 设置工作目录（会自动创建）

# ===== 暴露端口 =====
EXPOSE 80                        # 暴露端口（文档性，不实际映射）
EXPOSE 80/tcp                    # 指定协议
EXPOSE 80/udp

# ===== 用户切换 =====
USER node                        # 指定运行用户
USER 1000:1000                   # 指定 UID:GID

# ===== 健康检查 =====
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1
HEALTHCHECK NONE                 # 禁用健康检查

# ===== 入口与命令 =====
ENTRYPOINT ["python", "app.py"]  # 入口点（不可被覆盖）
ENTRYPOINT ["nginx", "-g", "daemon off;"]
CMD ["npm", "start"]            # 默认命令（可被 docker run 覆盖）
CMD ["--port", "8080"]          # 给 ENTRYPOINT 提供默认参数
CMD ["node", "server.js"]

# ===== 卷声明 =====
VOLUME ["/data"]                 # 声明挂载点
VOLUME /var/lib/mysql

# ===== 停止信号 =====
STOPSIGNAL SIGQUIT               # 停止信号
STOPSIGNAL SIGTERM

# ===== Shell 格式 vs Exec 格式 =====
RUN apt-get update               # Shell 格式：/bin/sh -c "apt-get update"
RUN ["apt-get", "update"]       # Exec 格式（推荐）：直接运行
CMD echo "Hello"                 # Shell 格式
CMD ["echo", "Hello"]           # Exec 格式

# ===== 多阶段构建示例 =====
FROM golang:1.22 AS builder
WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o app .

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
COPY --from=builder /build/app /app
EXPOSE 8080
USER 1001
ENTRYPOINT ["/app"]
```

### 7.2 .dockerignore

```dockerignore
.git
node_modules
target/
__pycache__/
*.pyc
.env
*.log
.gitignore
Dockerfile
README.md
```

### 7.3 最佳实践

```dockerfile
# 1. 使用具体版本标签而非 latest
FROM python:3.12-slim

# 2. 设置工作目录
WORKDIR /app

# 3. 先复制依赖文件，利用缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 后复制源码（依赖层缓存）
COPY . .

# 5. 使用非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 6. 设置健康检查
HEALTHCHECK CMD curl -f http://localhost:8000/health || exit 1

# 7. 多阶段构建减小镜像
FROM node:20-alpine AS builder
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
EXPOSE 80
```

---

## 8. 构建命令 (Build)

### 8.1 构建选项

```bash
# 基本构建
docker build -t my-app:v1.0 .                    # 从当前目录 Dockerfile
docker build -t my-app:v1.0 -f Dockerfile.prod . # 指定 Dockerfile
docker build -t my-app:v1.0 github.com/user/repo # 从 Git 仓库构建

# 缓存控制
docker build --no-cache -t my-app:v1.0 .         # 不使用缓存
docker build --cache-from registry.example.com/cache:latest .  # 使用远程缓存
docker build --cache-to type=registry,ref=registry.example.com/cache:latest .  # 推送缓存

# BuildKit 高级功能
DOCKER_BUILDKIT=1 docker build .                 # 手动启用 BuildKit
docker build --progress=plain .                  # 显示详细构建输出
docker build --progress=auto .                   # 自动输出模式
docker build --quiet -t my-app:v1.0 .            # 安静模式
docker build --network=host .                    # 使用主机网络构建
docker build --shm-size 256m .                   # 共享内存大小

# 输出选项
docker build -o type=local,dest=./output .       # 导出文件系统
docker build -o type=tar,dest=out.tar .          # 导出为 tar
docker build --iidfile image-id.txt .             # 输出镜像 ID 到文件

# 标签与元数据
docker build -t app:v1.0 -t app:latest .         # 多个标签
docker build --label version=1.0 --label env=prod .
```

### 8.2 Buildx（多架构构建）

```bash
# 安装/查看
docker buildx version                            # 查看 Buildx 版本
docker buildx ls                                 # 列出构建器
docker buildx inspect                            # 检查当前构建器

# 创建构建器
docker buildx create --name mybuilder            # 创建构建器
docker buildx create --use --name mybuilder      # 创建并使用
docker buildx use mybuilder                      # 切换到构建器
docker buildx rm mybuilder                       # 删除构建器
docker buildx stop mybuilder                     # 停止构建

# 多架构构建
docker buildx build --platform linux/amd64,linux/arm64 -t user/app:latest --push .
docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7 -t user/app:latest .

# 本地加载多架构镜像
docker buildx build --platform linux/amd64,linux/arm64 -t user/app:latest --load .

# 使用 bake（构建编排）
docker buildx bake                                # 从 docker-bake.hcl 构建
docker buildx bake -f docker-bake.hcl
```

---

## 9. 日志与监控

### 9.1 容器日志

```bash
# 查看日志
docker logs <容器>                     # 查看所有日志
docker logs -f <容器>                  # 实时追踪（类似 tail -f）
docker logs --tail 100 <容器>          # 最近 100 行
docker logs --tail 0 -f <容器>         # 只看新日志
docker logs --since "2026-07-17T10:00:00" <容器>  # 指定起始时间
docker logs --since 5m <容器>          # 最近 5 分钟
docker logs --until "2026-07-17T12:00:00" <容器>  # 指定结束时间
docker logs --timestamps <容器>        # 显示时间戳
docker logs -t <容器>                  # 时间戳简写
docker logs --details <容器>           # 显示额外详情

# 日志驱动配置
docker run --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 nginx
docker run --log-driver syslog --log-opt syslog-address=tcp://192.168.1.1:514 nginx
docker run --log-driver loki --log-opt loki-url=http://loki:3100/loki/api/v1/push nginx
docker run --log-driver fluentd --log-opt fluentd-address=localhost:24224 nginx

# 日志驱动类型
# - json-file   （默认，JSON 文件）
# - local       （更高效的本地格式）
# - journald    （systemd 日志）
# - syslog      （系统日志）
# - fluentd     （Fluentd 聚合）
# - awslogs     （CloudWatch）
# - gelf        （Graylog）
# - splunk      （Splunk）
# - loki        （Grafana Loki）
# - gcplogs     （Google Cloud Logging）
```

### 9.2 资源监控

```bash
# 实时监控
docker stats                         # 所有容器 CPU/内存/网络/磁盘
docker stats <容器>                  # 指定容器
docker stats --no-stream             # 单次快照
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"  # 自定义格式
docker stats --all                   # 包含已停止的

# 查看资源详情
docker inspect <容器> | grep -A 10 "HostConfig"  # 查看资源限制
docker inspect <容器> --format '{{.HostConfig.Memory}}'  # 内存限制
docker inspect <容器> --format '{{.HostConfig.CpuShares}}'  # CPU 份额
docker inspect <容器> --format '{{.HostConfig.CpusetCpus}}'  # CPU 亲和性

# 事件监控
docker events                        # 实时 Docker 事件流
docker events --filter "type=container" --filter "event=die"  # 过滤容器死亡事件
docker events --since 1h             # 过去 1 小时的事件
docker events --filter "container=my-nginx"  # 指定容器事件
```

### 9.3 日志管理

```bash
# 日志文件位置（默认）
# Linux: /var/lib/docker/containers/<container-id>/<container-id>-json.log
# Windows: 需通过 Docker 桌面设置查看

# 查看日志文件大小
ls -lh /var/lib/docker/containers/*/*-json.log

# 清空日志文件
truncate -s 0 /var/lib/docker/containers/*/*-json.log

# 日志轮转配置（docker-compose.yml）
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## 10. 资源清理

### 10.1 清理命令

```bash
# 一键清理（交互式）
docker system prune                  # 清理未使用的容器、网络、悬空镜像
docker system prune -a               # 清理所有未使用的镜像（含未使用的）
docker system prune --volumes        # 包含卷
docker system prune -f               # 强制清理（不提示）

# 分类清理
docker container prune               # 删除所有已停止的容器
docker container prune --filter "until=24h"  # 24 小时前停止的
docker image prune                   # 删除悬空镜像（dangling）
docker image prune -a                # 删除所有未使用的镜像
docker image prune -a --filter "until=24h"
docker volume prune                  # 删除所有未使用的卷
docker volume prune -f               # 强制删除
docker network prune                 # 删除所有未使用的网络
docker builder prune                 # 清理 BuildKit 缓存
docker builder prune --all           # 清理所有构建缓存
docker builder prune --filter "until=24h"  # 24 小时前的缓存
```

### 10.2 磁盘空间分析

```bash
# 查看磁盘使用情况
docker system df                     # 查看空间使用概览
docker system df -v                  # 详细查看（按镜像/容器/卷/构建缓存）
docker system df --format "table {{.Type}}\t{{.TotalCount}}\t{{.Size}}"

# 查找大文件
docker inspect <容器> -f '{{.GraphDriver.Data.MergedDir}}'  # 容器文件系统路径
du -sh /var/lib/docker/             # 查看 Docker 总空间
du -sh /var/lib/docker/volumes/*    # 各卷大小
ls -lhS /var/lib/docker/containers/*/*-json.log  # 日志文件大小排序
```

### 10.3 自动化清理

```bash
# 每天凌晨清理
0 3 * * * docker system prune -af --volumes >> /dev/null 2>&1

# 保留最近 24 小时的容器
0 2 * * * docker container prune --filter "until=24h" -f

# 保留最近 7 天的镜像
0 4 * * 0 docker image prune -a --filter "until=168h" -f
```

---

## 11. 系统管理

### 11.1 守护进程管理

```bash
# Linux systemd
sudo systemctl start docker          # 启动 Docker
sudo systemctl stop docker           # 停止 Docker
sudo systemctl restart docker        # 重启 Docker
sudo systemctl enable docker         # 开机自启
sudo systemctl disable docker        # 取消开机自启
sudo systemctl status docker         # 查看状态

# 查看 Docker 守护进程配置
sudo cat /etc/docker/daemon.json     # Docker 配置

# 示例 daemon.json
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" },
  "storage-driver": "overlay2",
  "exec-opts": ["native.cgroupdriver=systemd"],
  "data-root": "/data/docker",
  "registry-mirrors": ["https://mirror.ccs.tencentyun.com"],
  "insecure-registries": ["192.168.1.100:5000"],
  "live-restore": true
}
```

### 11.2 注册表与仓库

```bash
# 登录/登出
docker login                         # 登录 Docker Hub
docker login registry.example.com    # 登录私有仓库
docker login -u <用户名> -p <密码>   # 直接登录（不安全，仅脚本用）
docker logout                        # 登出

# 配置镜像加速（daemon.json）
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerproxy.com",
    "https://mirror.ccs.tencentyun.com"
  ]
}

# 私有仓库
docker run -d -p 5000:5000 --name registry registry:2  # 运行本地仓库
docker tag my-app:v1.0 localhost:5000/my-app:v1.0
docker push localhost:5000/my-app:v1.0
docker pull localhost:5000/my-app:v1.0
```

### 11.3 上下文与配置

```bash
docker context ls                    # 列出所有上下文
docker context create remote --docker "host=tcp://192.168.1.100:2375"
docker context use remote            # 切换到远程
docker context use default           # 切回本地
docker context inspect remote

docker system info                   # 系统信息
docker system info -f '{{.OSType}}'  # 特定字段
docker system info -f '{{.ServerVersion}}'
```

---

## 12. 安全与认证

### 12.1 容器安全

```bash
# 以非 root 运行
docker run --user 1000:1000 <镜像>
docker run --read-only <镜像>          # 只读文件系统
docker run --read-only --tmpfs /tmp <镜像>  # 只读 + 临时可写目录
docker run --cap-drop ALL --cap-add NET_BIND_SERVICE <镜像>  # 最小权限
docker run --security-opt no-new-privileges <镜像>  # 禁止提权
docker run --security-opt seccomp=seccomp-profile.json <镜像>  # 自定义 seccomp
docker run --security-opt apparmor=your-profile <镜像>

# 资源限制
docker run --cpus 2 --memory 512m --pids-limit 100 <镜像>
docker run --ulimit nofile=1024:2048 <镜像>
docker run --restart on-failure:5 <镜像>  # 最多重启 5 次
```

### 12.2 镜像安全

```bash
# 使用可信基础镜像
FROM docker.io/library/alpine:3.20
FROM alpine:3.20@sha256:abc123...  # 使用摘要确保不可变

# 扫描镜像漏洞
docker scout <镜像>                  # Docker Scout 扫描
docker scout quickview <镜像>        # 快速概览
docker scout cves <镜像>             # CVE 详情
docker scout recommendations <镜像>  # 修复建议

# 验证签名
docker trust inspect <镜像>          # 查看信任信息
docker trust sign <镜像>             # 对镜像签名
docker trust revoke <镜像>           # 撤销签名
```

### 12.3 内容信任

```bash
export DOCKER_CONTENT_TRUST=1       # 启用内容信任
docker pull nginx:latest            # 只拉取已签名的镜像
docker push my-app:v1.0             # 推送时自动签名
```

---

## 13. 多架构与 Buildx

```bash
# 查看当前架构
docker info | grep Architecture

# 模拟多平台
docker run --platform linux/arm64 --rm alpine uname -m
docker run --platform linux/amd64 --rm alpine uname -m

# 设置 QEMU 模拟（多架构构建）
docker run --privileged --rm tonistiigi/binfmt --install all
# 或 Docker Desktop 已内置

# 创建多架构构建器
docker buildx create --name multiarch --driver docker-container --use
docker buildx inspect --bootstrap    # 启动并检查支持架构

# 构建多架构镜像
docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7 \
  -t user/app:latest --push .

# 在本地加载特定架构
docker buildx build --platform linux/amd64 -o type=docker .

# 检查镜像支持哪些架构
docker manifest inspect <镜像>
docker buildx imagetools inspect <镜像>
```

---

## 14. 故障排查

### 14.1 容器问题

```bash
# 查看容器退出码
docker inspect <容器> --format '{{.State.ExitCode}}'
# 退出码说明: 0=正常, 1=应用错误, 137=SIGKILL, 139=SIGSEGV, 143=SIGTERM

# 查看容器日志
docker logs <容器> --tail=100

# 查看容器详细信息
docker inspect <容器>
docker inspect <容器> | jq '.[0].State'  # 查看状态（需安装 jq）

# 检查容器资源限制
docker inspect <容器> --format '{{json .HostConfig}}' | jq .

# 进入容器调试
docker exec -it <容器> /bin/sh
docker exec -it <容器> env                           # 查看环境变量
docker exec -it <容器> ps aux                        # 查看进程
docker exec -it <容器> df -h                         # 查看磁盘
docker exec -it <容器> ip addr                       # 查看网络
docker exec -it <容器> netstat -tlnp                 # 查看监听端口
```

### 14.2 网络问题

```bash
# 测试 DNS 解析
docker run --rm alpine nslookup google.com
docker run --rm alpine cat /etc/resolv.conf

# 测试网络连通性
docker run --rm alpine ping -c 3 8.8.8.8
docker run --rm alpine wget -O- http://example.com

# 查看网络配置
docker network inspect bridge
docker inspect <容器> --format '{{json .NetworkSettings}}' | jq .

# 追踪网络路径
docker run --rm --net=host nicolaka/netshoot traceroute google.com

# 抓包分析
docker run --rm --net=host nicolaka/netshoot tcpdump -i any port 80
```

### 14.3 磁盘问题

```bash
# 查看 Docker 磁盘使用
docker system df -v

# 查找大容器日志
find /var/lib/docker/containers -name "*-json.log" -exec ls -lh {} \; | sort -k5 -rh | head -10

# 清理日志
truncate -s 0 /var/lib/docker/containers/*/*-json.log

# 查看卷大小
du -sh /var/lib/docker/volumes/*

# 修改 Docker 数据目录
# 编辑 /etc/docker/daemon.json
{
  "data-root": "/mnt/docker-data"
}
```

### 14.4 常见错误解决

```bash
# Error: "no space left on device"
docker system prune -af --volumes

# Error: "port is already allocated"
netstat -tlnp | grep <端口号>           # 找出占用进程
# 或修改映射端口

# Error: "permission denied" (Linux)
sudo usermod -aG docker $USER           # 将用户加入 docker 组
# 然后重新登录

# Error: "daemon not running"
sudo systemctl start docker
sudo dockerd                            # 前台启动，查看日志

# Error: "exec format error" (架构不匹配)
docker run --platform linux/amd64 <镜像>

# Error: "container name already exists"
docker rm <容器>                        # 删除同名容器
docker run --rm ...                     # 下次用 --rm 自动清理

# Error: "Cannot connect to the Docker daemon"
# Windows: 启动 Docker Desktop
# Linux: sudo systemctl start docker
# macOS: 启动 Docker Desktop

# Error: "unauthorized: access token expired"
docker login                            # 重新登录
```

---

## 15. 实用技巧与脚本

### 15.1 一键命令

```bash
# 停止并删除所有容器
docker stop $(docker ps -q) && docker rm $(docker ps -aq)

# 删除所有悬空镜像
docker rmi $(docker images -f dangling=true -q)

# 清理所有未使用资源
docker system prune -af --volumes

# 查看所有容器 IP
docker inspect -f '{{.Name}} - {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $(docker ps -aq)

# 查看容器资源排名（按内存）
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}" | sort -k2 -h

# 查看容器资源排名（按 CPU）
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}" | sort -k2 -h

# 在宿主机和容器之间复制文件
docker cp <容器>:/app/logs/app.log ./app.log
docker cp ./config.json <容器>:/app/config.json
```

### 15.2 Bash 别名

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
alias dps='docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
alias dpsa='docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
alias di='docker images'
alias dlog='docker logs -f --tail 100'
alias dexec='docker exec -it'
alias dstop='docker stop $(docker ps -q)'
alias drm='docker rm $(docker ps -aq)'
alias dprune='docker system prune -af --volumes'
alias dcu='docker compose up -d'
alias dcd='docker compose down'
alias dcl='docker compose logs -f'
alias dcb='docker compose build'
alias dcr='docker compose restart'
alias dce='docker compose exec'
alias dtop='docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"'
alias dsize='docker system df -v'

# Docker 清理脚本
cleanup-docker() {
  echo "=== Stopping all containers ==="
  docker stop $(docker ps -q) 2>/dev/null
  echo "=== Removing all containers ==="
  docker rm $(docker ps -aq) 2>/dev/null
  echo "=== Cleaning unused resources ==="
  docker system prune -af --volumes
  echo "=== Done ==="
}
```

### 15.3 容器内工具镜像

```bash
# 网络调试
docker run --rm --net=host nicolaka/netshoot

# 数据库客户端
docker run --rm -it --net=host postgres:16 psql -h localhost -U postgres
docker run --rm -it --net=host redis:7-alpine redis-cli -h localhost

# 压测
docker run --rm --net=host petenathan/httpress -n 10000 -c 100 http://localhost:8080/

# 证书解析
docker run --rm -v $(pwd)/certs:/certs alpine openssl x509 -in /certs/cert.pem -text
```

### 15.4 性能优化

```bash
# 使用 overlay2 存储驱动（daemon.json）
{
  "storage-driver": "overlay2",
  "storage-opts": ["overlay2.override_kernel_check=true"]
}

# 限制日志大小
docker run --log-opt max-size=10m --log-opt max-file=3 nginx

# 使用 Alpine 或 slim 镜像
FROM python:3.12-slim  # 而不是 python:3.12
FROM alpine:3.20       # 或 alpine

# 多阶段构建减小镜像
# 见 Dockerfile 章节

# 合并 RUN 命令减少层
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# 利用构建缓存
# 先复制 requirements.txt，再复制源码
```

### 15.5 Windows 特有命令

```powershell
# PowerShell 命令
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker logs --tail 100 -f $(docker ps -q)

# Docker Desktop 设置
# 设置 → Resources → Advanced (CPU/内存)
# 设置 → Docker Engine (daemon.json 配置)
# 设置 → Kubernetes (启用/禁用)

# WSL2 集成
wsl --set-version <distro> 2       # 设置 WSL2
# Docker Desktop → Settings → Resources → WSL Integration

# 路径转换（PowerShell）
docker run -v ${PWD}:/app alpine   # PowerShell 自动转换路径
docker run -v "$(pwd):/app" alpine # 或者
```

---

## 附录：命令速查表

### 容器
| 命令 | 说明 |
|------|------|
| `docker run -d -p 80:80 nginx` | 后台运行 Nginx |
| `docker exec -it <c> /bin/sh` | 进入容器 |
| `docker logs -f <c>` | 实时日志 |
| `docker stop <c> && docker rm <c>` | 停止并删除 |
| `docker cp <c>:/path ./path` | 复制文件 |

### 镜像
| 命令 | 说明 |
|------|------|
| `docker pull nginx:alpine` | 拉取镜像 |
| `docker build -t app:v1 .` | 构建镜像 |
| `docker push user/app:v1` | 推送镜像 |
| `docker rmi <img>` | 删除镜像 |
| `docker save <img> -o file.tar` | 导出镜像 |

### Compose
| 命令 | 说明 |
|------|------|
| `docker compose up -d` | 后台启动所有服务 |
| `docker compose down` | 停止并删除服务 |
| `docker compose logs -f` | 实时日志 |
| `docker compose exec web /bin/sh` | 进入服务容器 |
| `docker compose build` | 构建所有服务 |

### 系统
| 命令 | 说明 |
|------|------|
| `docker system df` | 磁盘使用 |
| `docker system prune -af` | 全面清理 |
| `docker stats` | 资源监控 |
| `docker info` | 系统信息 |
| `docker events` | 事件流 |
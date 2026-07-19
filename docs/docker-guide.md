# Docker 操作指南

> 涵盖 Docker 基础概念、镜像构建、容器运行、日志查看、卷映射和日常运维命令。

---

## 目录

1. [Docker 基础概念](#1-docker-基础概念)
2. [安装与配置](#2-安装与配置)
3. [镜像管理](#3-镜像管理)
4. [容器管理](#4-容器管理)
5. [端口映射](#5-端口映射)
6. [项目镜像构建](#6-项目镜像构建)
7. [容器运行（项目）](#7-容器运行项目)
8. [数据管理](#8-数据管理)
9. [日志管理](#9-日志管理)
10. [网络管理](#10-网络管理)
11. [Dockerfile 详解](#11-dockerfile-详解)
12. [运维命令速查](#12-运维命令速查)
13. [常见问题排查](#13-常见问题排查)

---

## 1. Docker 基础概念

### 1.1 什么是 Docker

Docker 是一个开源的容器化平台，基于 **Go 语言** 开发。它通过 **操作系统级虚拟化**，将应用及其依赖打包在轻量级容器中运行。

**Docker 与虚拟机对比：**

| 特性     | Docker 容器                | 虚拟机 (VM) |
| -------- | -------------------------- | ----------- |
| 启动速度 | 秒级（毫秒级）             | 分钟级      |
| 镜像大小 | MB 级（如 CentOS 仅 179M） | GB 级       |
| 性能     | 接近原生                   | 有一定损耗  |
| 密度     | 单机 100~1000+ 个容器      | 单机数十个  |
| 内核     | 共享宿主机内核             | 独立内核    |
| 隔离性   | 进程级隔离                 | 完全隔离    |

### 1.2 三大核心概念

```
┌─────────────────────────────────────────────────┐
│                   Repository                    │
│  ┌───────┐  ┌───────┐  ┌───────┐               │
│  │ Image │  │ Image │  │ Image │  ...           │
│  │  1.0  │  │  1.1  │  │  2.0  │               │
│  └───┬───┘  └───┬───┘  └───┬───┘               │
│      │          │          │                    │
│      ▼          ▼          ▼                    │
│  ┌─────────────────────────────────────────┐    │
│  │           docker pull / push             │    │
│  └─────────────────────────────────────────┘    │
│          │                                      │
│          ▼                                      │
│  ┌─────────────────────────────────────────┐    │
│  │  docker run ───▶ Container (运行实例)     │    │
│  │  docker commit ◀─── 容器保存为新镜像       │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

| 概念                        | 类比                 | 说明                                  |
| --------------------------- | -------------------- | ------------------------------------- |
| **镜像 (Image)**      | 类的模板 / ISO 文件  | 只读模板，包含运行环境和应用代码      |
| **容器 (Container)**  | 类的实例 / 运行的 VM | 镜像的运行实例，可读写，可启停        |
| **仓库 (Repository)** | Git 仓库 / 应用商店  | 存储和分发镜像的场所（如 Docker Hub） |

### 1.3 镜像分层与 UnionFS

Docker 使用 **UnionFS（联合文件系统）** 实现镜像分层存储：

```
┌──────────────────────────────────┐
│  容器层 (Container Layer) 可读写   │ ◄── 容器运行时产生
├──────────────────────────────────┤
│  镜像层 4: 应用代码与配置         │ ◄── 如 webapp 源码
├──────────────────────────────────┤
│  镜像层 3: 基础运行环境           │ ◄── 如 JDK、Python 运行时
├──────────────────────────────────┤
│  镜像层 2: OS 基础包             │ ◄── 如 apt/yum 安装的包
├──────────────────────────────────┤
│  镜像层 1: 基础镜像 (base image)  │ ◄── 如 Ubuntu、Alpine
├──────────────────────────────────┤
│      内核 (Kernel)               │ ◄── 共享宿主机内核
└──────────────────────────────────┘
```

- **分层复用**：多个镜像共享相同底层，节省磁盘空间
- **写时复制 (Copy-on-Write)**：容器层修改时，从镜像层复制数据到容器层
- **容器层**：每个容器有自己的读写层，删除容器时该层随之删除

---

## 2. 安装与配置

### 2.1 安装 Docker

**CentOS 6.8：**

```bash
yum install -y epel-release
yum install -y docker-io
```

**CentOS 7+：**

```bash
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io
```

**Ubuntu / Debian：**

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
```

**启动 Docker：**

```bash
service docker start      # CentOS 6
systemctl start docker    # CentOS 7+ / systemd 系统
systemctl enable docker   # 设置开机自启
```

### 2.2 验证安装

```bash
docker version     # 查看客户端和服务端版本
docker info        # 查看 Docker 系统信息（容器数、镜像数、驱动等）
docker --help      # 查看所有命令帮助
```

### 2.3 配置镜像加速器

在国内使用 Docker Hub 拉取镜像较慢，建议配置镜像加速器：

**CentOS 6.8** → 编辑 `/etc/sysconfig/docker`：

```bash
other_args="--registry-mirror=https://<your-mirror>"
```

**CentOS 7+** → 编辑 `/etc/docker/daemon.json`：

```json
{
  "registry-mirrors": ["https://<your-mirror>"]
}
```

常用镜像加速器：

- **阿里云**：`https://<你的ID>.mirror.aliyuncs.com`
- **中科大**：`https://docker.mirrors.ustc.edu.cn`
- **网易**：`http://hub-mirror.c.163.com`

配置后重启：

```bash
systemctl daemon-reload
systemctl restart docker
ps -ef | grep docker    # 确认配置已生效
```

### 2.4 项目环境准备

```bash
# 检查 Docker 版本
docker --version            # >= 24.0
docker compose version      # >= 2.20

# 确保 Docker 运行中
docker info
```

### 2.5 项目环境变量

```bash
# 复制环境变量模板
cp .env.example .env
# 编辑 .env 文件，修改密码等敏感信息
```

`.env` 文件内容：

| 变量                  | 默认值                                     | 说明                     |
| --------------------- | ------------------------------------------ | ------------------------ |
| `DB_PASSWORD`       | `pipeline123`                            | PostgreSQL 密码          |
| `MINIO_USER`        | `pipeline`                               | MinIO 用户名             |
| `MINIO_PASSWORD`    | `pipeline123`                            | MinIO 密码               |
| `RABBITMQ_USER`     | `pipeline`                               | RabbitMQ 用户名          |
| `RABBITMQ_PASSWORD` | `pipeline123`                            | RabbitMQ 密码            |
| `JWT_SECRET`        | `pipeline-secret-key-change-in-prod`     | JWT 签名密钥（生产更换） |
| `CALLBACK_TOKEN`    | `pipeline-callback-token-change-in-prod` | 回调认证令牌（生产更换） |
| `CALLBACK_BASE_URL` | `http://pipeline-admin:8080`             | Python 回调 Java 的地址  |

---

## 3. 镜像管理

### 3.1 查看镜像

```bash
docker images
# 或
docker image ls
```

**常用选项：**

| 选项           | 说明                         |
| -------------- | ---------------------------- |
| `-a`         | 列出所有镜像（含中间层镜像） |
| `-q`         | 只显示镜像 ID                |
| `--digests`  | 显示镜像摘要信息             |
| `--no-trunc` | 不截断输出                   |

```bash
# 过滤项目相关镜像
docker images | grep pipeline

# 查看镜像详情
docker inspect pipeline-admin:latest

# 查看镜像历史（分层信息）
docker history pipeline-admin:latest
```

### 3.2 搜索镜像

```bash
docker search [OPTIONS] <关键词>
```

**常用选项：**

| 选项                 | 说明                     |
| -------------------- | ------------------------ |
| `--no-trunc`       | 不截断输出               |
| `--limit N`        | 限制输出 N 个结果        |
| `--filter stars=N` | 仅显示收藏数 >= N 的镜像 |

```bash
# 示例：搜索 MySQL 镜像
docker search mysql
docker search --filter stars=1000 --limit 10 nginx
```

### 3.3 拉取镜像

```bash
docker pull <镜像名>[:TAG]
# 不指定 TAG 默认为 latest
```

```bash
# 示例
docker pull centos
docker pull tomcat:9-jdk17
docker pull mysql:5.6
docker pull redis:3.2
docker pull python:3.12-slim
docker pull eclipse-temurin:17-jre-alpine
```

### 3.4 删除镜像

```bash
docker rmi <镜像名或ID>         # 删除指定镜像
docker rmi -f <镜像名或ID>      # 强制删除（容器正在使用该镜像时需要加 -f）
docker rmi <镜像1:TAG> <镜像2:TAG>   # 删除多个镜像
docker rmi -f $(docker images -qa)   # 删除所有镜像
```

**注意**：需先删除依赖该镜像的容器（`docker rm`），再删除镜像。

### 3.5 docker commit —— 从容器创建镜像

```bash
docker commit [OPTIONS] <容器ID> [仓库名[:TAG]]
```

| 选项   | 说明               |
| ------ | ------------------ |
| `-a` | 作者信息           |
| `-m` | 提交描述信息       |
| `-p` | 提交时暂停容器运行 |

**典型工作流：**

```bash
# 1. 启动一个容器并进行修改
docker run -it --name my-tomcat tomcat /bin/bash
# 在容器内修改（如删除 docs、添加应用等）

# 2. 将修改后的容器提交为新镜像
docker commit -m="del tomcat docs" -a="developer" my-tomcat my-tomcat:1.0

# 3. 基于新镜像启动容器
docker run -it -p 9999:8080 my-tomcat:1.0
docker run -it -p 8989:8080 tomcat  # 原始镜像仍存在，对比测试
```

---

## 4. 容器管理

### 4.1 创建并运行容器

```bash
docker run [OPTIONS] <镜像名> [COMMAND] [ARG...]
```

**常用选项：**

| 选项                  | 说明                                               |
| --------------------- | -------------------------------------------------- |
| `-d`                | 后台运行容器，返回容器 ID                          |
| `-i`                | 交互模式，保持 stdin 打开（通常与`-t` 同时使用） |
| `-t`                | 分配伪终端（pseudo-TTY）                           |
| `--name`            | 指定容器名称                                       |
| `-p`                | 端口映射（宿主机端口:容器端口）                    |
| `-P`                | 随机映射容器暴露端口到宿主机高位端口               |
| `-v`                | 挂载数据卷                                         |
| `-e`                | 设置环境变量                                       |
| `--rm`              | 容器退出后自动删除                                 |
| `--privileged=true` | 赋予容器扩展权限（如挂载目录需要）                 |

**示例：**

```bash
# 交互式运行 CentOS
docker run -it centos /bin/bash

# 后台运行 Tomcat
docker run -d -p 6666:8080 tomcat

# 运行 MySQL 并设置环境变量
docker run -d -p 3306:3306 --name mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -v /data/mysql:/var/lib/mysql \
  mysql:5.6
```

### 4.2 查看容器

```bash
docker ps [OPTIONS]
```

| 选项           | 说明                     |
| -------------- | ------------------------ |
| (无选项)       | 仅显示运行中的容器       |
| `-a`         | 显示所有容器（含已退出） |
| `-l`         | 显示最近创建的容器       |
| `-n N`       | 显示最近 N 个创建的容器  |
| `-q`         | 只显示容器 ID            |
| `--no-trunc` | 不截断输出               |

### 4.3 容器生命周期

```
         docker create
              │
              ▼
    ┌─────────────────┐
    │    Created      │
    └────────┬────────┘
             │ docker start
             ▼
    ┌─────────────────┐
    │    Running      │◄────┐
    └──┬──────┬───────┘     │
       │      │             │
       │      ▼             │
       │  ┌─────────┐       │
       │  │ Paused   │──────┘ docker unpause
       │  └──────────┘
       │ docker stop / kill
       ▼
    ┌─────────────────┐
    │    Exited       │
    └────────┬────────┘
             │ docker start (可重新启动)
             │ docker rm  (彻底删除)
             ▼
    ┌─────────────────┐
    │     Removed     │
    └─────────────────┘
```

```bash
# 启动
docker start <容器ID或名称>

# 重启
docker restart <容器ID或名称>

# 停止（给进程发送 SIGTERM，优雅停止）
docker stop <容器ID或名称>

# 强制杀死（发送 SIGKILL）
docker kill <容器ID或名称>

# 暂停/恢复
docker pause <容器ID或名称>
docker unpause <容器ID或名称>
```

### 4.4 退出容器

| 方式                 | 操作                         | 效果                       |
| -------------------- | ---------------------------- | -------------------------- |
| **退出并停止** | 输入`exit` 或按 `Ctrl+D` | 容器停止运行（`Exited`） |
| **退出不停止** | 按`Ctrl+P+Q`               | 容器继续在后台运行         |

### 4.5 删除容器

```bash
docker rm <容器ID或名称>
docker rm -f <容器ID或名称>       # 强制删除（即使正在运行）
docker rm -f $(docker ps -a -q)  # 删除所有容器
docker ps -a -q | xargs docker rm # 同上（另一种写法）
```

### 4.6 docker exec vs docker attach

```bash
# exec —— 在运行中的容器执行命令（推荐）
docker exec -it <容器ID> /bin/bash      # 进入容器并启动新 shell
docker exec -it <容器ID> ls -l /tmp      # 在容器内执行命令

# attach —— 连接到容器的主进程
docker attach <容器ID>                  # 连接到容器主进程（会看到该进程的输出）
```

| 对比       | `docker exec`                     | `docker attach`                 |
| ---------- | ----------------------------------- | --------------------------------- |
| 启动新进程 | ✅ 是，启动一个新的进程             | ❌ 不，连接到主进程               |
| 退出影响   | `exit` 仅退出该进程，容器继续运行 | `exit` 会停止容器（主进程退出） |
| 调试场景   | ✅ 推荐使用                         | ❌ 容易误停容器                   |
| 多终端场景 | 多个终端可同时 exec                 | 多个 attach 会共享同一终端流      |

### 4.7 docker cp —— 文件复制

```bash
# 从容器复制到宿主机
docker cp <容器ID>:<容器内路径> <宿主机路径>

# 从宿主机复制到容器
docker cp <宿主机路径> <容器ID>:<容器内路径>

# 示例：复制 Tomcat 日志到宿主机
docker cp tomcat-container:/usr/local/tomcat/logs ./logs
```

### 4.8 docker top —— 查看容器进程

```bash
docker top <容器ID或名称>
# 类似于在容器内执行 ps -ef，但直接在宿主机查看
```

---

## 5. 端口映射

### 5.1 端口映射格式

```bash
# -P：自动映射（随机分配宿主机高位端口）
docker run -d -P tomcat
# 查看映射：docker ps → PORTS 显示 0.0.0.0:32768->8080/tcp

# -p：指定映射（4种格式）
docker run -d -p <ip>:<hostPort>:<containerPort>  # 完整格式
docker run -d -p <ip>::<containerPort>             # 宿主机随机端口
docker run -d -p <hostPort>:<containerPort>        # 最常用
docker run -d -p <containerPort>                   # 宿主机随机端口（同 ::containerPort）
```

**示例：**

```bash
docker run -d -p 6666:8080 tomcat          # 宿主机 6666 → 容器 8080
docker run -d -p 192.168.1.100:8888:8080    # 绑定到特定 IP
docker run -d -p 8888:8080 -p 8443:443     # 映射多个端口
```

### 5.2 项目端口映射

| 服务                | 容器端口 | 主机端口 | 说明             |
| ------------------- | -------- | -------- | ---------------- |
| PostgreSQL          | 5432     | 5432     | 数据库           |
| Redis               | 6379     | 6379     | 缓存             |
| MinIO API           | 9000     | 9000     | 对象存储 API     |
| MinIO Console       | 9001     | 9001     | MinIO 管理后台   |
| RabbitMQ AMQP       | 5672     | 5672     | 消息队列         |
| RabbitMQ Management | 15672    | 15672    | 消息队列管理后台 |
| pipeline-admin      | 8080     | 8080     | Java 后端 API    |
| frontend            | 80       | 80       | 前端管理界面     |
| ai-gateway          | 8001     | 8001     | AI 网关          |
| script-service      | 8002     | 8002     | 脚本服务         |
| prompt-service      | 8003     | 8003     | 分镜服务         |
| video-service       | 8004     | 8004     | 视频服务         |
| voice-service       | 8005     | 8005     | 配音服务         |
| ffmpeg-service      | 8006     | 8006     | 剪辑服务         |
| image-service       | 8007     | 8007     | 图片服务         |

---

## 6. 项目镜像构建

### 6.1 一键构建所有服务

```bash
# 构建所有镜像（不含缓存）
docker compose build --no-cache

# 构建所有镜像（利用缓存，推荐日常使用）
docker compose build

# 并行构建
docker compose build --parallel

# 构建并立即启动
docker compose up -d --build
```

### 6.2 构建单个服务镜像

```bash
# Java 后台
docker compose build pipeline-admin

# AI Gateway
docker compose build ai-gateway

# 脚本生成服务
docker compose build script-service

# 其他服务同理
docker compose build prompt-service
docker compose build video-service
docker compose build voice-service
docker compose build ffmpeg-service
docker compose build image-service
docker compose build frontend
```

### 6.3 查看镜像

```bash
docker images | grep pipeline

# 查看镜像详情
docker inspect pipeline-admin:latest

# 查看镜像分层历史（调试 Dockerfile 时有用）
docker history pipeline-admin:latest
```

### 6.4 Dockerfile 位置

| 服务           | Dockerfile 路径                           | 基础镜像                                                                                    |
| -------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------- |
| pipeline-admin | `pipeline-manager/Dockerfile`           | `maven:3.9-eclipse-temurin-17-alpine`（构建）→ `eclipse-temurin:17-jre-alpine`（运行） |
| ai-gateway     | `ai-services/gateway/Dockerfile`        | `python:3.12-slim`                                                                        |
| script-service | `ai-services/script-service/Dockerfile` | `python:3.12-slim`                                                                        |
| prompt-service | `ai-services/prompt-service/Dockerfile` | `python:3.12-slim`                                                                        |
| video-service  | `ai-services/video-service/Dockerfile`  | `python:3.12-slim`                                                                        |
| voice-service  | `ai-services/voice-service/Dockerfile`  | `python:3.12-slim`                                                                        |
| ffmpeg-service | `ai-services/ffmpeg-service/Dockerfile` | `python:3.12-slim`                                                                        |
| image-service  | `ai-services/image-service/Dockerfile`  | `python:3.12-slim`                                                                        |
| frontend       | `frontend/Dockerfile`                   | `node:20-alpine`（构建）→ `nginx:alpine`（运行）                                       |

---

## 7. 容器运行（项目）

### 7.1 启动所有服务

```bash
# 后台启动所有服务
docker compose up -d

# 查看启动状态
docker compose ps -a
```

### 7.2 分步启动

```bash
# 1. 启动基础设施（数据库、缓存、消息队列、对象存储）
docker compose up -d postgres redis minio rabbitmq

# 2. 等待 PostgreSQL 就绪（约 10-30 秒）
docker compose logs postgres

# 3. 启动 Java 后台
docker compose up -d pipeline-admin

# 4. 启动 Python AI 服务
docker compose up -d ai-gateway script-service prompt-service
docker compose up -d video-service voice-service ffmpeg-service image-service

# 5. 启动前端
docker compose up -d frontend
```

### 7.3 启动单个服务

```bash
# 启动并查看日志（前台模式）
docker compose up <service-name>

# 后台启动
docker compose up -d <service-name>

# 重启单个服务
docker compose restart <service-name>

# 示例
docker compose up -d script-service
```

### 7.4 停止服务

```bash
# 停止所有服务（保留容器、卷、网络）
docker compose stop

# 停止并删除所有容器和网络（保留数据卷）
docker compose down

# 停止所有并删除数据卷（⚠️ 数据会丢失）
docker compose down -v

# 停止单个服务
docker compose stop <service-name>

# 停止并删除单个服务容器
docker compose rm -fs <service-name>
```

### 7.5 完整生命周期命令

```bash
# 首次部署（构建 + 启动）
docker compose up -d --build

# 重新构建并重启某个服务
docker compose build <service-name>
docker compose up -d <service-name>

# 滚动重启所有服务
docker compose restart

# 查看所有服务状态
docker compose ps -a
```

---

## 8. 数据管理

### 8.1 数据卷（Volume）概述

Docker 中的数据持久化方案：

| 方式                                | 适用场景                     | 特点                                    |
| ----------------------------------- | ---------------------------- | --------------------------------------- |
| **bind mount** (主机目录映射) | 源码挂载、日志持久化         | 直接映射主机目录，依赖主机路径          |
| **volume** (Docker 管理卷)    | 数据库数据、不受主机路径影响 | Docker 管理，`docker volume` 命令操作 |
| **tmpfs mount** (内存挂载)    | 敏感数据、临时文件           | 数据存内存，容器退出即丢失              |

```
宿主机                           容器
┌──────────┐                ┌───────────┐
│  /data/mysql  │◄──bind──►│ /var/lib/mysql  │
│              │   mount    │               │
│  Volume:     │◄──volumes─►│ /data          │
│  mysql_data  │            │               │
│              │            │               │
│  /dev/shm    │◄─tmpfs────►│ /tmp/cache     │
└──────────┘                └───────────┘
```

### 8.2 数据卷容器

数据卷容器是一种在容器间共享数据的模式，使用 `--volumes-from`：

```bash
# 1. 创建数据卷容器（通常不运行任何服务）
docker run -it --name dc01 \
  -v /dataContainer1 \
  -v /dataContainer2 \
  zzyy/centos /bin/bash

# 2. 从数据卷容器挂载（dc02）
docker run -it --name dc02 \
  --volumes-from dc01 \
  zzyy/centos /bin/bash
# dc02 自动继承 dc01 的 /dataContainer1 和 /dataContainer2

# 3. dc03 同样继承
docker run -it --name dc03 \
  --volumes-from dc01 \
  zzyy/centos /bin/bash
```

**特性：**

- **链式继承**：dc03 可以从 dc02 挂载，dc02 从 dc01 挂载
- **删除不影响**：删除 dc01 后，dc02/dc03 上的数据卷仍可正常访问
- **解除依赖**：dc04 可以从 dc03 挂载，即使 dc03 是继承自 dc01 的

### 8.3 项目卷映射一览

```yaml
# docker-compose.yml 中的卷映射
volumes:
  postgres_data:     # PostgreSQL 数据（Docker volume）
  redis_data:        # Redis 数据
  minio_data:        # MinIO 对象存储
  rabbitmq_data:     # RabbitMQ 数据
```

### 8.4 主机目录映射（日志持久化）

```yaml
# 日志文件映射到主机（便于调试）
volumes:
  - ./ai-services/gateway/logs:/app/logs       # AI Gateway 日志
  - ./ai-services/script-service/logs:/app/logs  # 脚本服务日志
  - ./ai-services/prompt-service/logs:/app/logs  # 分镜服务日志
  - ./ai-services/video-service/logs:/app/logs   # 视频服务日志
  - ./ai-services/voice-service/logs:/app/logs   # 配音服务日志
  - ./ai-services/ffmpeg-service/logs:/app/logs  # 剪辑服务日志
  - ./ai-services/image-service/logs:/app/logs   # 图片服务日志
```

### 8.5 卷管理命令

```bash
# 列出所有数据卷
docker volume ls

# 查看卷详情
docker volume inspect content-pipeline-system_postgres_data

# 创建卷
docker volume create my-volume

# 备份卷
docker run --rm \
  -v content-pipeline-system_postgres_data:/source \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/postgres_data.tar.gz -C /source .

# 恢复卷
docker run --rm \
  -v content-pipeline-system_postgres_data:/target \
  -v $(pwd)/backup:/backup \
  alpine tar xzf /backup/postgres_data.tar.gz -C /target

# 清理未使用的卷
docker volume prune

# 删除特定卷
docker volume rm content-pipeline-system_postgres_data
```

### 8.6 数据卷挂载的高级选项

```bash
# 只读挂载（容器内无法修改主机数据）
docker run -it -v /宿主机路径:/容器路径:ro centos

# 通过 Dockerfile VOLUME 指令声明的匿名卷
# 在 docker inspect 中可以查看挂载详情
docker inspect <容器ID> \
  --format '{{json .Mounts}}' | jq .
```

### 8.7 数据库备份与恢复

```bash
# 备份 PostgreSQL
docker exec -t postgres pg_dump -U pipeline pipeline > backup_$(date +%Y%m%d).sql

# 恢复 PostgreSQL
cat backup.sql | docker exec -i postgres psql -U pipeline pipeline

# 备份 MinIO 数据（卷备份，需先停止服务）
docker run --rm \
  -v content-pipeline-system_minio_data:/source \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/minio_$(date +%Y%m%d).tar.gz -C /source .
```

---

## 9. 日志管理

### 9.1 查看所有服务日志

```bash
# 实时查看所有日志
docker compose logs -f

# 最近 100 行
docker compose logs --tail=100
```

### 9.2 查看单个服务日志

```bash
# 实时追踪
docker compose logs -f <service-name>

# 最近 50 行并追踪
docker compose logs --tail=50 -f pipeline-admin

# 仅查看错误日志
docker compose logs pipeline-admin 2>&1 | grep -i error

# 导出日志到文件
docker compose logs pipeline-admin > pipeline-admin.log
```

### 9.3 查看文件日志（Python 服务）

Python 服务使用 `RotatingFileHandler`，日志文件映射到主机目录：

```bash
# AI Gateway 日志
tail -f ./ai-services/gateway/logs/info.log
tail -f ./ai-services/gateway/logs/error.log

# 脚本服务日志
tail -f ./ai-services/script-service/logs/info.log

# 其他服务同理
tail -f ./ai-services/prompt-service/logs/info.log
tail -f ./ai-services/video-service/logs/info.log
```

日志文件按级别分离：`debug.log`、`info.log`、`warning.log`、`error.log`（每个最大 30MB，保留 3 个备份）。

### 9.4 查看 Java 日志

```bash
# Java 日志（按天滚动）
tail -f ./pipeline-manager/logs/info.2026-07-17.log
tail -f ./pipeline-manager/logs/error.2026-07-17.log

# Docker 日志
docker compose logs --tail=50 -f pipeline-admin
```

### 9.5 Docker 日志命令速查

```bash
# 关键 Docker 日志命令
docker logs <container-id>              # 查看容器日志
docker logs -f <container-id>           # 实时追踪
docker logs --tail=100 <container-id>   # 最近 100 行
docker logs --since 5m <container-id>   # 最近 5 分钟
docker logs --since 2026-07-19T00:00:00 <container-id>  # 从指定时间
docker logs --until 2026-07-19T12:00:00 <container-id>  # 到指定时间
docker logs -t <container-id>           # 显示时间戳

# 获取容器 ID
docker ps | grep <service-name>
```

### 9.6 Docker 日志驱动配置

```yaml
# docker-compose.yml 中限制日志大小
logging:
  driver: "json-file"
  options:
    max-size: "10m"     # 每个日志文件最大 10MB
    max-file: "3"       # 保留 3 个历史文件
```

---

## 10. 网络管理

### 10.1 Docker 网络模式

| 网络模式             | 说明                        | 适用场景     |
| -------------------- | --------------------------- | ------------ |
| `bridge`（默认）   | 容器间通过 docker0 网桥通信 | 单机容器互访 |
| `host`             | 容器直接使用宿主机网络栈    | 性能要求高   |
| `none`             | 无网络                      | 高安全场景   |
| `container:<name>` | 共享另一个容器的网络栈      | sidecar 模式 |

```bash
docker network ls              # 查看所有网络
docker network inspect <name>  # 查看网络详情
docker network create <name>   # 创建自定义网络
docker network connect <name> <container>  # 将容器加入网络
docker network disconnect <name> <container>  # 将容器移出网络
```

### 10.2 项目网络拓扑

```bash
# 查看项目网络
docker network ls | grep pipeline

# 查看网络详情（查看各容器 IP 地址）
docker network inspect content-pipeline-system_default
```

所有服务在同一网络中通过服务名互相访问：

| 源服务              | 目标服务       | 地址                                          |
| ------------------- | -------------- | --------------------------------------------- |
| Java 后台           | PostgreSQL     | `postgres:5432`                             |
| Java 后台           | Redis          | `redis:6379`                                |
| Java 后台           | RabbitMQ       | `rabbitmq:5672`                             |
| Python 服务         | RabbitMQ       | `rabbitmq:5672`                             |
| Python 服务         | MinIO          | `minio:9000`                                |
| Python 服务         | AI Gateway     | `ai-gateway:8001`                           |
| Python → Java 回调 | pipeline-admin | `pipeline-admin:8080/api/v1/tasks/callback` |

### 10.3 网络调试

```bash
# 进入容器测试网络连通性
docker exec -it <container-name> ping postgres
docker exec -it pipeline-admin curl http://ai-gateway:8001/health
docker exec -it script-service curl http://pipeline-admin:8080/api/v1/health

# 查看容器 IP 地址
docker inspect <container-name> | grep IPAddress
# 或使用格式输出
docker inspect <container-name> --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'

# 查看容器端口映射
docker port <container-name>
```

---

## 11. Dockerfile 详解

### 11.1 什么是 Dockerfile

Dockerfile 是一个文本文件，包含一系列指令，Docker 引擎读取后自动构建镜像。

**构建流程：**

```
docker build -f <Dockerfile路径> -t <镜像名:标签> <构建上下文路径>
```

```
Dockerfile ──▶ docker build ──▶ 临时容器1 ──▶ 临时容器2 ──▶ ... ──▶ 最终镜像
                (每条指令创建      (RUN 指令       (COPY 指令
                 一个中间层)        安装软件)         复制文件)
```

### 11.2 Dockerfile 指令全集

| 指令            | 说明                                 | 示例                                                  |
| --------------- | ------------------------------------ | ----------------------------------------------------- |
| `FROM`        | 指定基础镜像（必须是第一条指令）     | `FROM python:3.12-slim`                             |
| `MAINTAINER`  | 维护者信息（已弃用，推荐 LABEL）     | `MAINTAINER author@example.com`                     |
| `LABEL`       | 添加元数据标签                       | `LABEL version="1.0"`                               |
| `RUN`         | 在构建时执行命令                     | `RUN apt-get update && apt-get install -y curl`     |
| `CMD`         | 容器启动时的默认命令                 | `CMD ["python", "app.py"]`                          |
| `ENTRYPOINT`  | 容器入口点                           | `ENTRYPOINT ["docker-entrypoint.sh"]`               |
| `EXPOSE`      | 声明容器运行时监听的端口             | `EXPOSE 8080`                                       |
| `ENV`         | 设置环境变量                         | `ENV JAVA_HOME=/usr/lib/jvm/java-17`                |
| `WORKDIR`     | 设置工作目录                         | `WORKDIR /app`                                      |
| `COPY`        | 复制本地文件到镜像（推荐）           | `COPY ./app /app`                                   |
| `ADD`         | 复制文件或 URL，自动解压 tar         | `ADD app.tar.gz /app`                               |
| `VOLUME`      | 声明匿名卷挂载点                     | `VOLUME /data`                                      |
| `USER`        | 指定运行用户                         | `USER appuser`                                      |
| `ARG`         | 构建参数（构建时可用，运行时不可用） | `ARG VERSION=1.0`                                   |
| `ONBUILD`     | 当此镜像被用作基础镜像时执行         | `ONBUILD COPY . /app`                               |
| `STOPSIGNAL`  | 停止容器时的信号                     | `STOPSIGNAL SIGQUIT`                                |
| `HEALTHCHECK` | 健康检查指令                         | `HEALTHCHECK CMD curl --fail http://localhost:8080` |
| `SHELL`       | 覆盖默认的 shell                     | `SHELL ["/bin/bash", "-c"]`                         |

### 11.3 RUN、CMD、ENTRYPOINT 对比

| 指令           | 构建阶段      | 运行阶段        | 可被覆盖                  | 用途                 |
| -------------- | ------------- | --------------- | ------------------------- | -------------------- |
| `RUN`        | ✅ 构建时执行 | ❌              | —                        | 安装软件包、配置环境 |
| `CMD`        | ❌            | ✅ 容器启动执行 | ✅`docker run` 命令覆盖 | 提供默认执行命令     |
| `ENTRYPOINT` | ❌            | ✅ 容器启动执行 | ❌（需`--entrypoint`）  | 固定容器入口程序     |

**组合用法：**

```dockerfile
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
# 运行：docker run my-nginx            → nginx -g daemon off;
# 运行：docker run my-nginx -g "..."    → nginx -g "..."
```

### 11.4 COPY vs ADD

| 对比         | `COPY`             | `ADD`                     |
| ------------ | -------------------- | --------------------------- |
| 建议         | ✅**推荐使用** | ❌ 除非需要自动解压         |
| 复制本地文件 | ✅                   | ✅                          |
| 从 URL 拉取  | ❌                   | ✅（建议用 curl/wget 替代） |
| 自动解压 tar | ❌                   | ✅                          |
| 透明度       | 高，行为清晰         | 低，隐含自动解压            |

### 11.5 多阶段构建

```dockerfile
# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# 第二阶段：运行（更小的最终镜像）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

**好处：** 最终镜像只包含运行时依赖，不包含构建工具（Maven、JDK 等），大幅减小体积。

### 11.6 项目典型 Dockerfile 示例

**Java 服务（pipeline-admin）多阶段构建：**

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Python 服务（AI 服务典型 Dockerfile）：**

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8002
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002"]
```

**前端服务（Nginx 部署）：**

```dockerfile
# 构建阶段
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 12. 运维命令速查

### 12.1 监控与健康检查

```bash
# 查看所有服务状态
docker compose ps -a

# 查看容器运行时长、端口、名称等信息
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看资源使用（CPU、内存、网络、磁盘）
docker stats
docker stats <container-name>

# 查看正在运行的容器进程
docker top <container-name>

# 健康检查端点
curl http://localhost:8080/api/v1/health          # Java 后台
curl http://localhost:8001/health                  # AI Gateway
curl http://localhost:8002/health                  # Script Service
curl http://localhost:8003/health                  # Prompt Service
```

### 12.2 进入容器

```bash
# 进入运行中的容器
docker exec -it <container-name> /bin/bash
docker exec -it <container-name> /bin/sh   # Alpine 镜像用 sh

# 在容器内执行命令
docker exec <container-name> ls /app
docker exec pipeline-admin ps aux
docker exec pipeline-admin env | grep PIPELINE

# 以 root 用户进入（容器默认可能不是 root）
docker exec -u root -it <container-name> /bin/bash
```

### 12.3 清理

```bash
# 停止并删除所有容器
docker compose down

# 停止、删除容器和网络（保留卷）
docker compose down

# 完全清理（含卷，⚠️ 数据将丢失）
docker compose down -v

# 清理未使用的镜像（-a 删除所有未被使用的镜像）
docker image prune -a

# 清理停止的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 全面清理（⚠️ 容器/镜像/网络/卷全部清理）
docker system prune -a --volumes

# 查看磁盘使用量
docker system df
```

### 12.4 容器故障排查

```bash
# 查看容器日志
docker compose logs <service-name>

# 检查容器退出原因（Exit Code）
docker inspect <container-name> --format '{{.State.ExitCode}}'
# 退出码说明：0=正常退出  1=应用错误  137=SIGKILL(9)  143=SIGTERM(15)

# 查看容器启动命令
docker inspect <container-name> --format '{{.Config.Cmd}}'

# 查看容器环境变量
docker inspect <container-name> --format '{{range .Config.Env}}{{println .}}{{end}}'

# 查看挂载卷
docker inspect <container-name> --format '{{json .Mounts}}' | jq .

# 查看容器资源限制
docker inspect <container-name> --format '{{json .HostConfig.Memory}}' | jq .

# 测试网络连通性
docker exec <container-name> ping -c 3 postgres

# 查看容器完整元数据（排障时最全面）
docker inspect <container-name>
```

---

## 13. 常见问题排查

### 13.1 容器启动顺序依赖

```yaml
# docker-compose.yml 中的 depends_on 配置
depends_on:
  postgres:
    condition: service_healthy   # 等待健康检查通过
  redis:
    condition: service_started   # 等待启动
  rabbitmq:
    condition: service_started
  minio:
    condition: service_started
  ai-gateway:
    condition: service_started   # Python 服务等待 Gateway
```

`depends_on` 仅在 **docker compose up** 时生效，若分开启动需手动确保顺序。

### 13.2 端口冲突

```bash
# 检查端口占用
netstat -ano | grep -E "8080|5432|6379|5672|9000|8001"

# 修改主机端口映射（编辑 docker-compose.yml）
ports:
  - "8081:8080"  # 将主机 8081 映射到容器 8080
```

### 13.3 日志文件过大

```yaml
# Docker 日志限制（在 docker-compose.yml 中配置）
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

查看当前日志占用：

```bash
# 查看各容器日志文件大小
docker inspect <container> --format='{{.LogPath}}' | xargs ls -lh
```

### 13.4 数据库连接失败

```bash
# 检查 PostgreSQL 是否就绪
docker compose logs postgres

# 手动测试连接
docker exec -it postgres psql -U pipeline -d pipeline -c "SELECT 1"
```

### 13.5 容器退出码参考

| 退出码           | 含义             | 常见原因                         |
| ---------------- | ---------------- | -------------------------------- |
| `0`            | 正常退出         | 进程完成运行                     |
| `1`            | 应用错误         | 启动失败、配置错误、端口被占     |
| `137` (128+9)  | 被 SIGKILL 杀死  | OOM（内存不足）、`docker kill` |
| `139` (128+11) | 段错误 (SIGSEGV) | 内存越界、兼容性问题             |
| `143` (128+15) | 被 SIGTERM 终止  | `docker stop` 正常停止         |
| `255`          | 未知错误         | 通常表示应用崩溃                 |

### 13.6 权限问题

```bash
# 挂载主机目录时遇到 permission denied
# 添加 --privileged=true 参数
docker run -it --privileged=true -v /data:/data centos

# Volume 挂载的文件属于 root，需注意用户 ID 映射
# 解决方法：在 Dockerfile 中使用 USER 指令指定运行用户
```

### 13.7 docker-compose 常用命令速查

```bash
docker compose up -d          # 后台启动服务
docker compose down           # 停止并删除容器、网络
docker compose ps             # 查看服务状态
docker compose logs -f        # 查看日志
docker compose build          # 构建镜像
docker compose pull           # 拉取镜像
docker compose restart        # 重启服务
docker compose stop           # 停止服务
docker compose start          # 启动已停止的服务
docker compose rm             # 删除已停止的容器
docker compose exec <svc> <cmd>  # 在运行的服务中执行命令
docker compose config         # 验证并查看 docker-compose 配置
```

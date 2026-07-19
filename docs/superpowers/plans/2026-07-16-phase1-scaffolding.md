# Phase 1: 项目骨架 + 基础设施 + 核心后端

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 搭建可运行的项目骨架，基础设施容器化，核心数据模型 + REST API

**架构:** Spring Boot 3 + MyBatis Plus + PostgreSQL + Redis + MinIO + RabbitMQ

**前置:** Docker Desktop 已安装，Java 17 JDK 已配置

---

### Task 1: Docker Compose 基础设施

**Files:**
- Create: `docker-compose.yml`
- Create: `init.sql`
- Create: `.env.example`

- [ ] **Step 1: 创建 `.env.example`**

```bash
touch docker-compose.yml init.sql .env.example
```

- [ ] **Step 2: 编写 Docker Compose**

写入 `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: pipeline
      POSTGRES_USER: pipeline
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pipeline"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_USER:-pipeline}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD:-pipeline123}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-pipeline}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-pipeline123}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"

volumes:
  postgres_data:
  redis_data:
  minio_data:
  rabbitmq_data:
```

- [ ] **Step 3: 编写 init.sql**

写入 `init.sql`:

```sql
-- 创建 MinIO buckets（需要启动后手动创建或使用 MinIO Client）
-- 以下是数据库初始化脚本

-- 用户角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    code        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
    nickname    VARCHAR(50),
    role_id     BIGINT REFERENCES sys_role(id),
    status      VARCHAR(10) DEFAULT 'ENABLED',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 选题表
CREATE TABLE IF NOT EXISTS topic (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    source      VARCHAR(50),
    source_url  VARCHAR(500),
    hot_score   INT DEFAULT 0,
    is_auto     BOOLEAN DEFAULT FALSE,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id            BIGSERIAL PRIMARY KEY,
    topic_id      BIGINT REFERENCES topic(id),
    title         VARCHAR(200) NOT NULL,
    script_id     BIGINT,
    status        VARCHAR(20) NOT NULL DEFAULT 'WAIT',
    progress      INT DEFAULT 0,
    error_message TEXT,
    created_by    BIGINT REFERENCES sys_user(id),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_created_at ON task(created_at);

-- 脚本表
CREATE TABLE IF NOT EXISTS script (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT REFERENCES topic(id),
    task_id     BIGINT REFERENCES task(id),
    title       VARCHAR(200),
    content     TEXT,
    subtitle    TEXT,
    prompt_template_id BIGINT,
    version     INT DEFAULT 1,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分镜表
CREATE TABLE IF NOT EXISTS storyboard (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES task(id),
    sequence    INT NOT NULL,
    duration    INT DEFAULT 5,
    scene_type  VARCHAR(50),
    character   VARCHAR(200),
    action      TEXT,
    environment VARCHAR(200),
    camera      VARCHAR(100),
    lighting    VARCHAR(100),
    style       VARCHAR(100),
    ai_prompt   TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_storyboard_task ON storyboard(task_id);

-- 素材表
CREATE TABLE IF NOT EXISTS material (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES task(id),
    storyboard_id   BIGINT REFERENCES storyboard(id),
    type            VARCHAR(20) NOT NULL,
    model           VARCHAR(50),
    url             VARCHAR(500),
    prompt          TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_material_task ON material(task_id);

-- 配音表
CREATE TABLE IF NOT EXISTS voice (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES task(id),
    voice_type  VARCHAR(50),
    voice_url   VARCHAR(500),
    speed       DECIMAL(3,2) DEFAULT 1.05,
    duration    INT,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 发布日志表
CREATE TABLE IF NOT EXISTS publish_log (
    id                BIGSERIAL PRIMARY KEY,
    task_id           BIGINT NOT NULL REFERENCES task(id),
    platform          VARCHAR(20) NOT NULL,
    account_id        BIGINT,
    title             VARCHAR(200),
    cover_url         VARCHAR(500),
    tags              VARCHAR(500),
    scheduled_at      TIMESTAMP,
    published_at      TIMESTAMP,
    status            VARCHAR(20) DEFAULT 'PENDING',
    platform_video_id VARCHAR(100),
    error_message     TEXT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_publish_log_platform ON publish_log(platform, status);

-- AI 模型配置表
CREATE TABLE IF NOT EXISTS ai_model_config (
    id                  BIGSERIAL PRIMARY KEY,
    model_name          VARCHAR(100) NOT NULL,
    provider            VARCHAR(50) NOT NULL,
    api_key_encrypted   VARCHAR(500),
    endpoint            VARCHAR(500),
    model_type          VARCHAR(50),
    default_params      TEXT,
    rate_limit          TEXT,
    enabled             BOOLEAN DEFAULT TRUE,
    weight              INT DEFAULT 10,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 平台账号表
CREATE TABLE IF NOT EXISTS platform_account (
    id                BIGSERIAL PRIMARY KEY,
    platform          VARCHAR(20) NOT NULL,
    account_name      VARCHAR(100) NOT NULL,
    cookies_encrypted TEXT,
    status            VARCHAR(10) DEFAULT 'ENABLED',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    variables   VARCHAR(500),
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认管理员
INSERT INTO sys_role (name, code, description) VALUES ('管理员', 'ADMIN', '系统管理员');
INSERT INTO sys_role (name, code, description) VALUES ('运营', 'OPERATOR', '内容运营');
INSERT INTO sys_role (name, code, description) VALUES ('编辑', 'EDITOR', '视频编辑');
```

- [ ] **Step 4: 启动基础设施**

Run:
```bash
docker compose up -d
```
Expected: 4 个容器启动成功

---

### Task 2: Maven 多模块项目骨架

**Files:**
- Create: `content-pipeline/pom.xml`
- Create: `content-pipeline/pipeline-common/pom.xml`
- Create: `content-pipeline/pipeline-common/src/main/java/com/pipeline/common/BaseEntity.java`
- Create: `content-pipeline/pipeline-common/src/main/java/com/pipeline/common/Result.java`
- Create: `content-pipeline/pipeline-framework/pom.xml`
- Create: `content-pipeline/pipeline-admin/pom.xml`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/PipelineAdminApplication.java`
- Create: `content-pipeline/pipeline-admin/src/main/resources/application.yml`
- Create: `content-pipeline/pipeline-admin/src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p content-pipeline/pipeline-common/src/main/java/com/pipeline/common
mkdir -p content-pipeline/pipeline-framework/src/main/java/com/pipeline/framework
mkdir -p content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin
mkdir -p content-pipeline/pipeline-admin/src/main/resources
```

- [ ] **Step 2: 编写父 POM**

写入 `content-pipeline/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.pipeline</groupId>
    <artifactId>content-pipeline</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>pipeline-common</module>
        <module>pipeline-framework</module>
        <module>pipeline-admin</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 3: 编写 pipeline-common 模块**

写入 `content-pipeline/pipeline-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.pipeline</groupId>
        <artifactId>content-pipeline</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>pipeline-common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

写入 `content-pipeline/pipeline-common/src/main/java/com/pipeline/common/BaseEntity.java`:

```java
package com.pipeline.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

写入 `content-pipeline/pipeline-common/src/main/java/com/pipeline/common/Result.java`:

```java
package com.pipeline.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 4: 编写 pipeline-framework 模块**

写入 `content-pipeline/pipeline-framework/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.pipeline</groupId>
        <artifactId>content-pipeline</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>pipeline-framework</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.pipeline</groupId>
            <artifactId>pipeline-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: 编写 pipeline-admin 入口模块**

写入 `content-pipeline/pipeline-admin/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.pipeline</groupId>
        <artifactId>content-pipeline</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>pipeline-admin</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.pipeline</groupId>
            <artifactId>pipeline-framework</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/PipelineAdminApplication.java`:

```java
package com.pipeline.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.pipeline")
public class PipelineAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(PipelineAdminApplication.class, args);
    }
}
```

写入 `content-pipeline/pipeline-admin/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: pipeline-admin
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/pipeline
    username: pipeline
    password: ${DB_PASSWORD:pipeline123}
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: pipeline
    password: pipeline123

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

写入 `content-pipeline/pipeline-admin/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pipeline
    username: pipeline
    password: pipeline123
  rabbitmq:
    username: pipeline
    password: pipeline123

logging:
  level:
    com.pipeline: DEBUG
```

- [ ] **Step 6: 编译验证**

Run:
```bash
cd content-pipeline
./mvnw clean compile
```
Expected: BUILD SUCCESS

---

### Task 3: 实体类 + MyBatis Plus Mapper

**Files:**
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Topic.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Task.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Script.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Storyboard.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Material.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Voice.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/PublishLog.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/TopicMapper.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/TaskMapper.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/ScriptMapper.java`

Note: 为缩短实现时间，先实现最核心的 3 个实体 + Mapper，其余实体可以在后续添加。

- [ ] **Step 1: 创建 Topic 实体**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Topic.java`:

```java
package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic")
public class Topic extends BaseEntity {
    private String title;
    private String source;
    private String sourceUrl;
    private Integer hotScore;
    private Boolean isAuto;
    private String status;
    private Long createdBy;
}
```

- [ ] **Step 2: 创建 Task 实体**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Task.java`:

```java
package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {
    private Long topicId;
    private String title;
    private Long scriptId;
    private String status;
    private Integer progress;
    private String errorMessage;
    private Long createdBy;
}
```

- [ ] **Step 3: 创建 Script 实体**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/Script.java`:

```java
package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("script")
public class Script extends BaseEntity {
    private Long topicId;
    private Long taskId;
    private String title;
    private String content;
    private String subtitle;
    private Long promptTemplateId;
    private Integer version;
    private String status;
    private Long createdBy;
}
```

- [ ] **Step 4: 创建 Mapper 接口**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/TopicMapper.java`:

```java
package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.Topic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
}
```

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/TaskMapper.java`:

```java
package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
```

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/ScriptMapper.java`:

```java
package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.Script;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScriptMapper extends BaseMapper<Script> {
}
```

- [ ] **Step 5: 编译验证**

Run:
```bash
cd content-pipeline
./mvnw clean compile
```
Expected: BUILD SUCCESS

---

### Task 4: 认证与安全

**Files:**
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/SecurityConfig.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/JwtAuthFilter.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/JwtUtil.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/SysUser.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/UserMapper.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/AuthController.java`

Note: JWT 认证，简化处理，不做复杂的 Spring Security 配置，先保证可用。

- [ ] **Step 1: 创建 SysUser 实体**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/entity/SysUser.java`:

```java
package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private Long roleId;
    private String status;
}
```

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/mapper/UserMapper.java`:

```java
package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
```

- [ ] **Step 2: 创建 JwtUtil**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/JwtUtil.java`:

```java
package com.pipeline.admin.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expiration = 86400000L; // 24h

    public JwtUtil(@Value("${jwt.secret:pipeline-secret-key-change-in-prod}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 3: 创建 JwtAuthFilter**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/JwtAuthFilter.java`:

```java
package com.pipeline.admin.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, username, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: 创建 SecurityConfig**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/SecurityConfig.java`:

```java
package com.pipeline.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 5: 创建 AuthController**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/AuthController.java`:

```java
package com.pipeline.admin.controller;

import com.pipeline.admin.config.JwtUtil;
import com.pipeline.admin.entity.SysUser;
import com.pipeline.admin.mapper.UserMapper;
import com.pipeline.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        SysUser user = userMapper.selectOne(
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
                        .lambdaQuery(SysUser.class)
                        .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Result.success(Map.of("token", token, "nickname", user.getNickname()));
    }

    @GetMapping("/me")
    public Result<?> me(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) return Result.error(401, "未认证");
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        return Result.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "roleId", user.getRoleId()
        ));
    }
}
```

- [ ] **Step 6: 添加健康检查端点**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/HealthController.java`:

```java
package com.pipeline.admin.controller;

import com.pipeline.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/v1/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
```

- [ ] **Step 7: 编译验证**

Run:
```bash
cd content-pipeline
./mvnw clean compile
```
Expected: BUILD SUCCESS

---

### Task 5: 核心 CRUD Controller

**Files:**
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/TopicController.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/TaskController.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/service/TaskService.java`
- Create: `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/service/TaskServiceImpl.java`

- [ ] **Step 1: 创建 TopicController**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/TopicController.java`:

```java
package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipeline.admin.entity.Topic;
import com.pipeline.admin.mapper.TopicMapper;
import com.pipeline.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {
    private final TopicMapper topicMapper;

    @GetMapping
    public Result<Page<Topic>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Topic> q = new LambdaQueryWrapper<Topic>()
                .eq(status != null && !status.isEmpty(), Topic::getStatus, status)
                .orderByDesc(Topic::getCreatedAt);
        return Result.success(topicMapper.selectPage(new Page<>(page, size), q));
    }

    @GetMapping("/{id}")
    public Result<Topic> get(@PathVariable Long id) {
        Topic topic = topicMapper.selectById(id);
        return topic != null ? Result.success(topic) : Result.error(404, "选题不存在");
    }

    @PostMapping
    public Result<Topic> create(@RequestBody Topic topic) {
        topic.setStatus("PENDING");
        topicMapper.insert(topic);
        return Result.success(topic);
    }

    @PutMapping("/{id}")
    public Result<Topic> update(@PathVariable Long id, @RequestBody Topic topic) {
        topic.setId(id);
        topicMapper.updateById(topic);
        return Result.success(topicMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicMapper.deleteById(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 2: 创建 TaskService**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/service/TaskService.java`:

```java
package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;

public interface TaskService {
    Task createTask(Long topicId, String title);
    void updateStatus(Long taskId, String status, Integer progress, String errorMessage);
}
```

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/service/TaskServiceImpl.java`:

```java
package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskMapper taskMapper;

    @Override
    public Task createTask(Long topicId, String title) {
        Task task = new Task();
        task.setTopicId(topicId);
        task.setTitle(title);
        task.setStatus("WAIT");
        task.setProgress(0);
        taskMapper.insert(task);
        return task;
    }

    @Override
    public void updateStatus(Long taskId, String status, Integer progress, String errorMessage) {
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(errorMessage);
        taskMapper.updateById(task);
    }
}
```

- [ ] **Step 3: 创建 TaskController**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/controller/TaskController.java`:

```java
package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.TaskService;
import com.pipeline.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskMapper taskMapper;
    private final TaskService taskService;

    @GetMapping
    public Result<Page<Task>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
                .eq(status != null && !status.isEmpty(), Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt);
        return Result.success(taskMapper.selectPage(new Page<>(page, size), q));
    }

    @GetMapping("/{id}")
    public Result<Task> get(@PathVariable Long id) {
        Task task = taskMapper.selectById(id);
        return task != null ? Result.success(task) : Result.error(404, "任务不存在");
    }

    @PostMapping
    public Result<Task> create(@RequestBody Task task) {
        Task created = taskService.createTask(task.getTopicId(), task.getTitle());
        return Result.success(created);
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) {
        taskService.updateStatus(id, "WAIT", 0, null);
        return Result.success(null);
    }
}
```

- [ ] **Step 4: 添加 MyBatis Plus 分页配置**

写入 `content-pipeline/pipeline-admin/src/main/java/com/pipeline/admin/config/MyBatisPlusConfig.java`:

```java
package com.pipeline.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRESQL));
        return interceptor;
    }
}
```

- [ ] **Step 5: 编译验证**

Run:
```bash
cd content-pipeline
./mvnw clean compile
```
Expected: BUILD SUCCESS

---

## 验证方法

- [ ] 启动 Docker Compose: `docker compose up -d`
- [ ] 启动 Spring Boot: `cd content-pipeline && ./mvnw spring-boot:run`
- [ ] 健康检查: `curl http://localhost:8080/api/v1/health`
- [ ] 登录: `curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'`
- [ ] 选题 CRUD 测试: `curl http://localhost:8080/api/v1/topics`
- [ ] 任务 CRUD 测试: `curl http://localhost:8080/api/v1/tasks`
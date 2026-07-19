# === 构建阶段 ===
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制 pom.xml 和源码
COPY pom.xml pom.xml
COPY src src/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B -q || true

# 编译打包（跳过测试）
RUN mvn package -DskipTests -q

# === 运行阶段 ===
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]